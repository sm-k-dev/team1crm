package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 사용자 상세 정보 클래스
 * 역할: 인증된 회원 엔티티(Member)의 정보를 Spring Security 인증 시스템에 연동
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Role enum 값(OWNER, ADMIN 등)을 Spring Security 권한 형식(ROLE_...)으로 변환
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        // 로그인 식별자로 이메일 사용
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (기본 true)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠김 여부 (기본 true)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 여부 (기본 true)
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부 (기본 true)
    }
}