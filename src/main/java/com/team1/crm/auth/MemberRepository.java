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
    /** 자기 계정을 제외하고 이름 중복을 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    // 이메일로 회원 정보를 조회하는 쿼리 메소드 (로그인 및 회원 조회용)
    Optional<Member> findByEmail(String email);

    // 이메일 중복 가입 방지를 위한 존재 여부 확인 메소드
    boolean existsByEmail(String email);

    /** 가입 방식과 관계없이 공백·대소문자를 통일하여 중복을 검사한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from Member m where lower(trim(m.email)) = :email")
    boolean existsByNormalizedEmail(@org.springframework.data.repository.query.Param("email") String email);
    /**
     * + 닉네임 중복 가입 방지  _추가 수정
     * 역할: 입력한 닉네임이 DB에 이미 존재하는지 확인합니다.
     *
     * @param nickname 회원가입 시 입력한 닉네임
     * @return 중복이면 true, 사용 가능하면 false
     * 작성자 : 황현우
     * 일시: 2026-11-11
     */
    boolean existsByNickname(String nickname);


    // 소셜 로그인(구글 등) 연동을 위한 조회 메소드
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

}
