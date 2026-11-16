package com.team1.crm.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * + 비밀번호 재설정 일회용 토큰 엔티티
 * 역할: 회원과 연결된 재설정 토큰의 SHA-256 해시값,
 *       발급 시간, 만료 시간, 사용 시간을 DB에 저장합니다.
 *       원본 토큰은 저장하지 않으며 발급 후 30분간 유효합니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_member", columnList = "member_id")
})
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    public PasswordResetToken(Member member, String tokenHash, LocalDateTime now) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = now.plusMinutes(30);
    }
    /** 사용되지 않았고 만료 전인 경우에만 true를 반환합니다. */
    public boolean isValid(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }
    /** 사용 시간을 기록해 같은 링크를 다시 사용하지 못하도록 합니다. */
    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
