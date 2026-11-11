package com.team1.crm.admin;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 인증과 같은 인코더로 사장님 계정을 만든다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOwnerService {
    private final AdminOwnerRepository repository;
    private final PasswordEncoder passwordEncoder;

    /** 이메일과 이름의 중복 필드를 반환한다. 중복이 없으면 null이다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public String duplicateField(AdminOwnerForm form) {
        if (repository.existsByEmail(form.getEmail().strip())) return "email";
        if (repository.existsByNickname(form.getNickname().strip())) return "nickname";
        return null;
    }

    /** 사장님 USER 역할을 서버에서 고정하고 비밀번호를 해시하여 저장한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Transactional
    public Long create(AdminOwnerForm form) {
        String duplicate = duplicateField(form);
        if (duplicate != null) throw new IllegalArgumentException(duplicate);
        Member member = new Member(form.getEmail().strip(), passwordEncoder.encode(form.getPassword()),
                form.getNickname().strip(), Role.USER);
        return repository.saveAndFlush(member).getId();
    }

    /** 사장님 역할의 대상만 화면용 정보로 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public AdminOwnerOption findOwner(Long ownerId) {
        Member owner = target(ownerId);
        return new AdminOwnerOption(owner.getId(), owner.getNickname(), owner.getEmail());
    }

    /** 현재 자기 이름은 허용하고 다른 계정 이름은 거절한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public boolean nicknameTaken(Long ownerId, String nickname) {
        return repository.existsByNicknameAndIdNot(nickname.strip(), ownerId);
    }

    /** 이름만 변경하고 이메일·비밀번호·권한·매장 관계는 유지한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Transactional
    public void updateNickname(Long ownerId, AdminOwnerEditForm form) {
        Member owner = target(ownerId);
        if (nicknameTaken(ownerId, form.getNickname())) throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        owner.updateNickname(form.getNickname().strip());
        repository.saveAndFlush(owner);
    }

    /** 없는 계정이나 관리자 계정은 사장님 수정 대상으로 허용하지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Member target(Long ownerId) {
        return repository.findById(ownerId).filter(member -> member.getRole() == Role.USER)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "사장님 계정을 찾을 수 없습니다."));
    }
}
