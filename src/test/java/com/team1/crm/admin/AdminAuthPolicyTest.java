package com.team1.crm.admin;

import com.team1.crm.auth.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** ADMIN 생성 계정의 공통 인증 정책·재설정 연동을 검증한다. 작성자: 김순민 / 작성일: 2026-11-16 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-auth-policy;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.mail.username=test@example.invalid", "app.base-url=http://localhost:8082"
})
@ActiveProfiles("test")
@Import({AdminOwnerService.class, PasswordResetService.class,
        com.team1.crm.auth.security.PasswordEncoderConfig.class, AdminAuthPolicyTest.MailConfig.class})
class AdminAuthPolicyTest {
    @Autowired AdminOwnerService owners;
    @Autowired MemberRepository members;
    @Autowired PasswordResetService reset;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;

    /** Form과 Service 모두 인증 담당 정책을 적용하는지 확인한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @ParameterizedTest
    @ValueSource(strings = {"abc1!", "abcdefgh!", "12345678!", "password123", "password 123!", "Password1234567890123!"})
    @DisplayName("ADMIN 등록의 Form과 저장은 일반 가입과 동일한 비밀번호 정책을 적용한다")
    void rejectsInvalidPassword(String password) {
        var form = form(); form.setPassword(password);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(form).stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }
        assertEquals("password", assertThrows(IllegalArgumentException.class, () -> owners.create(form)).getMessage());
        assertEquals(0, members.count());
    }

    /** Google 가입도 ADMIN 이메일 중복 범위에 포함한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @ParameterizedTest
    @ValueSource(strings = {"google@example.com", "GOOGLE@EXAMPLE.COM", " google@example.com "})
    @DisplayName("ADMIN은 Google 계정의 이메일 표기를 바꿔서 일반 계정을 생성할 수 없다")
    void googleEmailConflict(String email) {
        var google = new Member("Google@Example.com", "", "구글계정", Role.USER);
        google.linkSocial("google", "sub-123"); members.saveAndFlush(google);
        var form = form(); form.setEmail(email);
        assertEquals("email", owners.duplicateField(form));
        assertThrows(IllegalArgumentException.class, () -> owners.create(form));
        assertEquals(1, members.count());
        assertEquals("google", google.getProvider());
    }

    /** ADMIN 계정도 별도 재설정 구현 없이 기존 발급 서비스를 사용한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @Test @DisplayName("ADMIN 생성 계정은 정규화된 이메일로 기존 비밀번호 재설정 발급 대상이 된다")
    void createdOwnerCanRequestReset() {
        Long id = owners.create(form());
        assertEquals("owner@example.com", members.findById(id).orElseThrow().getEmail());
        reset.requestReset(" OWNER@EXAMPLE.COM ");
        assertEquals(1, tokens.count());
        assertEquals(id, tokens.findAll().getFirst().getMember().getId());
        // 테스트 트랜잭션은 롤백되므로 afterCommit SMTP 전송은 실행하지 않는다.
    }

    /** 재설정 결과·최초 비밀번호 차단·일회성을 실제 DB에서 확인한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @Test @DisplayName("ADMIN 생성 회원은 재설정 후 새 비밀번호만 사용하며 같은 링크는 재사용할 수 없다")
    void resetCreatedOwner() throws Exception {
        Long id = owners.create(form());
        Member member = members.findById(id).orElseThrow();
        String raw = "a".repeat(43);
        tokens.saveAndFlush(new PasswordResetToken(member, hash(raw), LocalDateTime.now()));
        reset.resetPassword(raw, "Changed-123!", "Changed-123!");
        em.flush(); em.clear();
        Member saved = members.findById(id).orElseThrow();
        assertTrue(encoder.matches("Changed-123!", saved.getPassword()));
        assertFalse(encoder.matches("Owner-test-123!", saved.getPassword()));
        assertEquals(Role.USER, saved.getRole()); assertNull(saved.getProvider());
        assertFalse(reset.isValidToken(raw));
        assertThrows(IllegalArgumentException.class, () -> reset.resetPassword(raw, "Another-123!", "Another-123!"));
    }

    /** Google 전용 회원을 비밀번호 재설정으로 일반 계정으로 전환하지 않는다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @Test @DisplayName("Google 전용 회원은 재설정 링크를 발급하지 않는다")
    void socialResetExcluded() {
        Member google = new Member("google@example.com", "", "구글계정", Role.USER);
        google.linkSocial("google", "sub-123"); members.saveAndFlush(google);
        reset.requestReset("google@example.com");
        assertEquals(0, tokens.count());
    }

    /** 공통 정책을 통과하는 관리자 등록 입력을 만든다. 작성자: 김순민 / 작성일: 2026-11-16 */
    private AdminOwnerForm form() {
        var form = new AdminOwnerForm(); form.setEmail(" OWNER@EXAMPLE.COM ");
        form.setNickname("사장님"); form.setPassword("Owner-test-123!"); return form;
    }

    /** DB에는 발급 서비스와 같은 해시만 저장한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    /** 외부 발송 없는 테스트 메일 의존성이다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @TestConfiguration
    static class MailConfig {
        @Bean JavaMailSender mailSender() { return mock(JavaMailSender.class); }
    }
}
