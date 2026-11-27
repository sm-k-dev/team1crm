package com.team1.crm.auth.security;

import com.team1.crm.auth.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** T19: 실제 SecurityConfig·JWT 필터·회원 API, 테스트 H2만 사용. 김순민, 2026-11-27. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jwt-protection;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "server.port=0",
        "jwt.secret=jwt-test-only-signing-key-at-least-32-bytes-long",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.mail.username=test@example.invalid", "spring.mail.password=test-only",
        "spring.ai.model.chat=none", "spring.ai.model.embedding=none",
        "spring.ai.model.image=none", "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none", "spring.ai.model.moderation=none"
})
@ActiveProfiles("test")
@Transactional
class JwtProtectionIntegrationTest {
    static final String SECRET = "jwt-test-only-signing-key-at-least-32-bytes-long";
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    @Autowired JwtTokenProvider tokens;
    MockMvc mvc;
    Member member;

    @BeforeEach void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        member = members.saveAndFlush(new Member("jwt-owner@test.invalid", "unused", "JWT검증", Role.USER));
    }

    String issued(long validity) {
        var principal = new CustomUserDetails(member);
        return new JwtTokenProvider(SECRET, validity).generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test void productionIssuerAllowsProtectedMemberApi() throws Exception {
        String token = issued(60_000);
        assertTrue(tokens.validateToken(token));
        mvc.perform(get("/api/members/me").servletPath("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(member.getEmail()))
                .andExpect(jsonPath("$.id").value(member.getId()));
    }

    @Test void expiredTokenReturns401WithoutAuthenticatedMember() throws Exception {
        String token = issued(-60_000);
        assertFalse(tokens.validateToken(token));
        mvc.perform(get("/api/members/me").servletPath("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test void payloadTamperingWithoutResigningReturns401() throws Exception {
        String[] parts = issued(60_000).split("\\.");
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"other@test.invalid\",\"exp\":4102444800}").getBytes(StandardCharsets.UTF_8));
        String token = parts[0] + "." + payload + "." + parts[2];
        assertFalse(tokens.validateToken(token));
        mvc.perform(get("/api/members/me").servletPath("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void tokenSignedByDifferentKeyReturns401() throws Exception {
        String token = Jwts.builder().subject(member.getEmail()).expiration(new Date(System.currentTimeMillis()+60_000))
                .signWith(Keys.hmacShaKeyFor("another-test-only-signing-key-with-32-bytes".getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/members/me").servletPath("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void malformedBearerReturns401() throws Exception {
        mvc.perform(get("/api/members/me").servletPath("/api/members/me").header("Authorization", "Bearer malformed.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test void userTokenCannotAccessAdminApi() throws Exception {
        mvc.perform(get("/api/admin/test-only").servletPath("/api/admin/test-only").header("Authorization", "Bearer " + issued(60_000)))
                .andExpect(status().isForbidden());
    }

    @Test void expirationClaimBoundaryUsesJwtSecondPrecision() {
        // 고정 clock으로 exp 경계 1ms 전/후를 검사한다. 실제 API 만료 거절은 별도 테스트가 담당한다.
        long expiry = 1_800_000_000_000L;
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject(member.getEmail()).expiration(new Date(expiry)).signWith(key).compact();
        assertDoesNotThrow(() -> Jwts.parser().verifyWith(key).clock(() -> new Date(expiry-1)).build().parseSignedClaims(token));
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> Jwts.parser().verifyWith(key).clock(() -> new Date(expiry+1)).build().parseSignedClaims(token));
    }
}
