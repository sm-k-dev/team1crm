package com.team1.crm.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * + 비밀번호 재설정 토큰 DB 접근 인터페이스
 * 역할: 해시 기반 토큰 조회, 회원별 최신 발급 기록 확인,
 *       미사용 토큰 조회 및 동시 재설정 방지용 잠금 조회를 담당합니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByMemberOrderByCreatedAtDesc(Member member);

    List<PasswordResetToken> findByMemberAndUsedAtIsNull(Member member);

    // 잠금을 얻기 전에 회원 PK만 읽어 같은 회원의 비밀번호 변경을 직렬화한다.
    @Query("select t.member.id from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<Long> findMemberIdByTokenHash(@Param("tokenHash") String tokenHash);


    /**
     * 토큰을 비관적 쓰기 잠금으로 조회합니다.
     * 회원 잠금과 함께 사용해 동시에 같은 링크가 제출되어도
     * 사용 여부를 검사한 후 비밀번호 변경을 처리합니다.
     * @param tokenHash 원본 토큰에서 계산한 SHA-256 해시
     * @return 해당 토큰이 없으면 Optional.empty()
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
