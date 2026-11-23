package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 회원 상세 정보 조회 서비스
 * 역할: 로그인 요청 시 이메일(username)을 통해 DB에서 회원 정보를 조회하여 인증 객체로 변환
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일을 기준으로 회원 조회 (없을 경우 예외 발생)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 회원을 찾을 수 없습니다: " + email));

        // 조회된 회원 엔티티를 CustomUserDetails로 감싸서 반환
        return new CustomUserDetails(member);
    }
}