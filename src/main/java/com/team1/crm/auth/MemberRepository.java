package com.team1.crm.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * + DB 회원 조회 & 저장 인터페이스
 * 역할: DB 회원 정보 데이터 접근 레이어
 * 작성자: 황현우
 * 일시:2026-11-05
 */


public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원 정보를 조회하는 쿼리 메소드 (로그인 및 회원 조회용)
    Optional<Member> findByEmail(String email);

    // 이메일 중복 가입 방지를 위한 존재 여부 확인 메소드
    boolean existsByEmail(String email);

    // 소셜 로그인(구글 등) 연동을 위한 조회 메소드
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

}
