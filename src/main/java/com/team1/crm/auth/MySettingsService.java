package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 본인 정보와 소유 매장만 수정한다. 작성자: 김순민 / 작성일: 2026-11-12 */
@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class MySettingsService {
    private final MemberRepository members;
    private final StoreRepository stores;

    /** 이메일과 이름만 화면에 전달한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    public Profile profile(Long memberId) {
        Member member = members.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Profile(member.getEmail(), member.getNickname());
    }
    /** 본인 이메일과 표시 이름이다. 작성자: 김순민 / 작성일: 2026-11-12 */
    public record Profile(String email, String nickname) {}

    /** DB 제약 위반 뒤 이름 중복 여부를 별도 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    public boolean nicknameTaken(Long memberId, String name) {
        return members.existsByNicknameAndIdNot(name.strip(), memberId);
    }

    /** 이름 변경은 비밀번호·이메일·권한을 변경하지 않는다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Transactional
    public void updateProfile(Long memberId, MyProfileForm form) {
        var member = members.findById(memberId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (nicknameTaken(memberId, form.getNickname()))
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        member.updateNickname(form.getNickname().strip());
        members.saveAndFlush(member);
    }

    /** 다른 매장의 주소를 조작하면 403으로 처리한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    public Store ownedStore(Long memberId, Long storeId) {
        Store store = stores.findByIdAndOwner_Id(storeId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "소유 매장만 수정할 수 있습니다."));
        store.requireOpen(); return store;
    }

    /** 잠금 후 소유권과 폐업 여부를 재검사하여 매장 정보를 수정한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Transactional
    public void updateStore(Long memberId, Long storeId, MyStoreForm form) {
        Store store = stores.findLockedById(storeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!store.getOwner().getId().equals(memberId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        store.requireOpen();
        if (stores.existsByIdAndClosedAtIsNotNull(storeId)) throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (stores.existsByOwner_IdAndNameAndClosedAtIsNullAndIdNot(memberId, form.getName().strip(), storeId))
            throw new IllegalArgumentException("내 다른 영업 매장에서 사용 중인 이름입니다.");
        store.update(form.getName().strip(), clean(form.getCategory()), clean(form.getAddress()), form.getPointRate());
        stores.flush();
    }
    /** 선택 문자열의 앞뒤 공백을 정리한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private String clean(String value) { return value == null ? null : value.strip(); }
}
