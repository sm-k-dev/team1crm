package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 비즈니스 로직 및 중복 검증
 * 역할: 회원가입, 중복 검증, 비밀번호 암호화 처리
 * 작성자: 황현우
 * 일시: 2026-11-05
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(MemberSignupForm form) {
        // 일반·Google 가입을 구분하지 않고 같은 이메일의 새 회원 생성을 막는다.
        // 작성자: 김순민 / 작성일: 2026-11-13
        String email = EmailAddress.normalize(form.getEmail());
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        if (memberRepository.existsByNormalizedEmail(email)) {
            throw new DuplicateMemberEmailException();
        }

        // 2. 닉네임 중복 검사
        if (memberRepository.existsByNickname(form.getNickname())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }
        // ============================================================
        // + 회원가입 비밀번호 규칙 공통화
        // 역할: 기존 회원가입에서 사용하던 8~20자 비밀번호 검증을
        //       PasswordPolicy로 분리해 비밀번호 재설정에서도 재사용합니다.
        // 작성자: 황현우 / 일시: 2026-11-16
        // ============================================================
        String password = form.getPassword();
        PasswordPolicy.validate(password);

        // 4. 비밀번호 암호화
        // SecurityConfig 등에 등록된 PasswordEncoder 사용
        String encodedPassword = passwordEncoder.encode(password);

        // 5. 회원 엔티티 생성 (기본 권한 USER)
        Member member = Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(form.getNickname())
                .role(Role.USER)
                .build();

        // DB에 저장 후 생성된 회원 PK(id) 반환
        Member savedMember = memberRepository.saveAndFlush(member);
        return savedMember.getId();
    }

    /**
     * 이메일 중복 여부 확인
     * true  : 이미 가입된 이메일
     * false : 사용 가능한 이메일
     */
    public boolean isEmailExists(String email) {
        // 사전 확인과 최종 저장에서 같은 중복 기준을 사용한다.
        // 작성자: 김순민 / 작성일: 2026-11-13
        return memberRepository.existsByNormalizedEmail(EmailAddress.normalize(email));
    }



}
