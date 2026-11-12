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
        // 1. 이메일 중복 검사
        if (memberRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException(
                    "이미 가입된 이메일입니다."
            );
        }

        // 2. 닉네임 중복 검사
        if (memberRepository.existsByNickname(form.getNickname())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        // 3. 비밀번호 보안 조건 검사
        // 영문, 숫자, 특수문자 각각 1개 이상 포함
        // 8~20자, 공백 사용 불가
        String password = form.getPassword();

        if (password == null ||
                !password.matches(
                        "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,20}$"
                )) {

            throw new IllegalArgumentException(
                    "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다. 공백은 사용할 수 없습니다."
            );
        }

        // 4. 비밀번호 암호화
        // SecurityConfig 등에 등록된 PasswordEncoder 사용
        String encodedPassword = passwordEncoder.encode(password);

        // 5. 회원 엔티티 생성 (기본 권한 USER)
        Member member = Member.builder()
                .email(form.getEmail())
                .password(encodedPassword)
                .nickname(form.getNickname())
                .role(Role.USER)
                .build();

        // 6. DB 저장 후 생성된 회원 ID 반환
        Member savedMember = memberRepository.save(member);

        return savedMember.getId();
    }

    /**
     * 이메일 중복 여부 확인
     * true  : 이미 가입된 이메일
     * false : 사용 가능한 이메일
     */
    public boolean isEmailExists(String email) {
        return memberRepository.existsByEmail(email);
    }



}
