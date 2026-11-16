package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** 이메일 검증 → 일회용 링크 발급 → 기존 Argon2 암호화 정책으로 비밀번호 변경. */
/**
 * + 비밀번호 재설정 핵심 서비스
 * 역할: 이메일 정규화 및 일반 회원 조회, Google 전용 회원 제외,
 *       5분 재발급 제한, 일회용 토큰 생성·해시 저장, 30분 만료 검사,
 *       Gmail SMTP를 이용한 HTML 메일 발송, Argon2 비밀번호 변경을 담당합니다.
 *       DB 트랜잭션 커밋 후 안내 메일을 발송합니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */


@Service
/**
 * 이메일 링크의 토큰 형식·만료 시간·사용 여부를 확인합니다.
 * @param rawToken 이메일 링크의 원본 토큰
 * @return 유효한 미사용 토큰이면 true
 */
@RequiredArgsConstructor
/**
 * 유효한 일회용 토큰으로 회원의 비밀번호를 변경합니다.
 * @param rawToken 재설정 링크의 원본 토큰
 * @param newPassword 새 비밀번호(암호화 전)
 * @param confirmPassword 새 비밀번호 확인값
 * @throws IllegalArgumentException 토큰·비밀번호 조건에 실패할 때
 */
@Slf4j
public class PasswordResetService {
    private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private final SecureRandom secureRandom = new SecureRandom();

    private final MemberRepository members;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 비밀번호 재설정 링크 발급 요청.
     * @param inputEmail 사용자가 입력한 이메일 주소
     * 참고: 등록되지 않은 이메일이나 Google 전용 계정에 대해서도
     *       외부 응답으로 계정 존재 여부를 공개하지 않습니다.
     */
    /** 계정 유무를 사용자에게 알리지 않는다. Google 전용 회원은 발급 대상이 아니다. */
    @Transactional
    public void requestReset(String inputEmail) {
        String email = EmailAddress.normalize(inputEmail);
        if (email == null || email.isBlank()) return;

        Member found = members.findByEmail(email).orElse(null);
        if (found == null) return;

        // 회원 단위 잠금: 동시에 여러 번 발급하거나 재설정할 때 상태가 꼬이지 않게 한다.
        Member member = members.findByIdForPasswordResetUpdate(found.getId()).orElse(null);
        if (member == null || member.getProvider() != null
                || member.getPassword() == null || member.getPassword().isBlank()) return;

        LocalDateTime now = LocalDateTime.now();
        boolean tooSoon = tokens.findTopByMemberOrderByCreatedAtDesc(member)
                .filter(last -> last.getCreatedAt().isAfter(now.minusMinutes(5)))
                .isPresent();
        if (tooSoon) return;

        // 이전 링크는 한 번에 무효화한다.
        tokens.findByMemberAndUsedAtIsNull(member).forEach(t -> t.markUsed(now));

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        tokens.save(new PasswordResetToken(member, sha256(rawToken), now));

        String url = baseUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
        String destination = member.getEmail();

        // DB에 링크가 저장된 후에만 메일을 발송한다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 1. HTML 메일 객체 생성
                    MimeMessage message = mailSender.createMimeMessage();

                    // [2026-11-16 / 황현우] HTML 재설정 버튼 메일 발송 오류 수정
                    // 일반 텍스트 + HTML 대체 본문을 setText(plainText, html)로 함께 제공하므로
                    // MimeMessageHelper의 Multipart 모드를 true로 활성화합니다.
                    // 수정 전: IllegalStateException 발생 → 수정 후: 네이버 메일 수신 확인
                    MimeMessageHelper helper =
                            new MimeMessageHelper(message, true,"UTF-8");

                    helper.setFrom(senderEmail);
                    helper.setTo(destination);
                    helper.setSubject("[StoreCRM] 비밀번호 재설정 안내");

                    // 2. 일반 텍스트 메일
                    String plainText =
                            "StoreCRM 비밀번호 재설정을 요청하셨습니다.\n\n"
                                    + "아래 링크를 이용해 새 비밀번호를 설정해 주세요.\n"
                                    + url + "\n\n"
                                    + "이 링크는 30분 동안 유효합니다.";

                    // 3. HTML 이메일 (클릭 가능한 버튼)
                    String safeUrl = HtmlUtils.htmlEscape(url);

                    String html = """
                <div style="font-family:Arial,sans-serif;
                            max-width:520px;margin:auto;
                            padding:24px;">

                    <h2>StoreCRM 비밀번호 재설정</h2>

                    <p>안녕하세요.</p>
                    <p>StoreCRM 비밀번호 재설정을 요청하셨습니다.</p>
                    <p>아래 버튼을 클릭해 새 비밀번호를 설정해 주세요.</p>

                    <div style="margin:30px 0;text-align:center;">
                        <a href="%s"
                           style="display:inline-block;
                                  background:#3165F4;
                                  color:#ffffff;
                                  padding:14px 28px;
                                  text-decoration:none;
                                  border-radius:8px;
                                  font-weight:bold;">
                            비밀번호 재설정하기
                        </a>
                    </div>

                    <p style="color:#666;font-size:13px;">
                        이 링크는 30분 동안 유효하며
                        한 번만 사용할 수 있습니다.
                    </p>

                    <p style="color:#666;font-size:13px;">
                        요청하지 않으셨다면 이 메일을 무시해 주세요.
                    </p>
                </div>
                """.formatted(safeUrl);

                    // 4. 일반 텍스트 + HTML 함께 전송
                    helper.setText(plainText, html);

                    // 5. 이메일 전송
                    mailSender.send(message);

                } catch (Exception ex) {
                    log.warn(
                            "비밀번호 재설정 메일 발송 실패: {}",
                            ex.getClass().getSimpleName()
                    );
                }
            }

        }); // TransactionSynchronization 등록 종료

    } // requestReset() 메서드 종료


    @Transactional(readOnly = true)
    public boolean isValidToken(String rawToken) {
        if (!isTokenFormatValid(rawToken)) return false;
        return tokens.findByTokenHash(sha256(rawToken))
                .filter(t -> t.isValid(LocalDateTime.now()))
                .isPresent();
    }

    /** 유효기간·재사용·비밀번호 정책을 모두 검사하고 기존 비밀번호를 교체한다. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!isTokenFormatValid(rawToken)) {
            throw new IllegalArgumentException("유효하지 않은 재설정 링크입니다.");
        }
        PasswordPolicy.validate(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("비밀번호 확인 값이 일치하지 않습니다.");
        }

        String hash = sha256(rawToken);
        Long memberId = tokens.findMemberIdByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 재설정 링크입니다."));
        Member member = members.findByIdForPasswordResetUpdate(memberId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 재설정 링크입니다."));
        PasswordResetToken token = tokens.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 재설정 링크입니다."));
        LocalDateTime now = LocalDateTime.now();
        if (!token.isValid(now)) {
            throw new IllegalArgumentException("이미 사용했거나 만료된 재설정 링크입니다.");
        }
        if (member.getProvider() != null) {
            throw new IllegalArgumentException("이 계정은 일반 비밀번호 변경을 지원하지 않습니다.");
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
        // 한 번 바꾸면 이 회원이 가진 모든 미사용 재설정 링크를 무효화한다.
        tokens.findByMemberAndUsedAtIsNull(member).forEach(t -> t.markUsed(now));
    }

    private static boolean isTokenFormatValid(String value) {
        return value != null && TOKEN_FORMAT.matcher(value).matches();
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", ex);
        }
    }
}
