package com.team1.crm.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.team1.crm.auth.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 관리자 매장 목록 조회를 담당한다. 접근 권한은 관리자 Controller와 보안 설정에서 확인한다.
 * 작성자: 김순민 / 작성일: 2026-11-11
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStoreService {
    private final AdminStoreRepository adminStoreRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    /** 전체 매장의 화면용 정보를 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public List<AdminStoreRow> findStores() {
        return adminStoreRepository.findStoreRows();
    }

    /** 현재 사장님 역할인 USER 계정만 선택지로 제공한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public List<AdminOwnerOption> findOwners() {
        return adminStoreRepository.findOwnerOptions(Role.USER);
    }

    /** 매장 없는 계정도 포함하여 사장님과 소유 매장들을 연결한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public List<AdminOwnerRow> findOwnerRows() {
        var storesByOwner = findStores().stream().filter(store -> store.ownerId() != null)
                .collect(java.util.stream.Collectors.groupingBy(AdminStoreRow::ownerId));
        return findOwners().stream().map(owner -> new AdminOwnerRow(owner.id(), owner.email(), owner.nickname(),
                List.copyOf(storesByOwner.getOrDefault(owner.id(), List.of())))).toList();
    }

    /** 기존 매장을 수정 입력값으로 변환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public AdminStoreForm findForm(Long storeId) {
        Store store = findStore(storeId);
        store.requireOpen();
        var form = new AdminStoreForm();
        form.setName(store.getName());
        form.setCategory(store.getCategory());
        form.setAddress(store.getAddress());
        form.setPointRate(store.getPointRate());
        form.setOwnerId(store.getOwner().getId());
        return form;
    }

    /** 기존 사장님에게 새 매장을 연결한다. 다중 매장 소유를 허용한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Transactional
    public Long create(AdminStoreForm form) {
        Member owner = findOwner(form.getOwnerId());
        return storeRepository.save(new Store(owner, form.getName().strip(),
                clean(form.getCategory()), clean(form.getAddress()), form.getPointRate())).getId();
    }

    /** 매장 정보와 소유자를 하나의 트랜잭션으로 변경한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Transactional
    public void update(Long storeId, AdminStoreForm form) {
        Store store = storeRepository.findLockedById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."));
        if (storeRepository.existsByIdAndClosedAtIsNotNull(storeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "폐업 매장은 변경할 수 없습니다.");
        }
        store.requireOpen();
        Member owner = findOwner(form.getOwnerId());
        store.update(form.getName().strip(), clean(form.getCategory()), clean(form.getAddress()), form.getPointRate());
        store.changeOwner(owner);
    }

    /** 존재하지 않는 매장은 404로 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Store findStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."));
    }

    /** 조작된 요청에서도 관리자 계정이나 없는 회원을 소유자로 지정하지 못한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Member findOwner(Long ownerId) {
        Member owner = ownerId == null ? null : memberRepository.findById(ownerId).orElse(null);
        if (owner == null || owner.getRole() != Role.USER) {
            throw new IllegalArgumentException("연결 가능한 사장님 계정을 선택해 주세요.");
        }
        return owner;
    }

    /** 선택 입력의 앞뒤 공백을 정리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private String clean(String text) { return text == null ? null : text.strip(); }
}
