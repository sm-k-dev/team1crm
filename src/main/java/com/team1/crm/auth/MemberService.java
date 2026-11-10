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
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 비밀번호 암호화 (Argon2 적용)
        String encodedPassword = passwordEncoder.encode(form.getPassword());

        // 회원 엔티티 생성 (빌더 패턴 활용, 기본 권한은 OWNER 또는 지정된 권한)
        Member member = Member.builder()
                .email(form.getEmail())
                .password(encodedPassword)
                .nickname(form.getNickname())
                .role(Role.USER)
                .build();

        // DB에 저장 후 생성된 회원 PK(id) 반환
        Member savedMember = memberRepository.save(member);
        return savedMember.getId();
    }
}
