package com.team1.crm.auth.security;

import com.team1.crm.auth.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 로그인 방식 분리와 기존 계정 자동 연결 차단을 검증한다. 작성자: 김순민 / 작성일: 2026-11-13 */
class LoginMethodTest {
    private MemberRepository repository;
    private ProviderManager authenticationManager;
    private CustomOAuth2UserService oauth;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 폼·JWT 로그인에서 공유하는 실제 비밀번호 인증 공급자를 구성한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @BeforeEach
    void setUp() {
        repository = mock(MemberRepository.class);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new CustomUserDetailsService(repository));
        provider.setPasswordEncoder(encoder);
        authenticationManager = new ProviderManager(provider);
        oauth = new CustomOAuth2UserService(repository);
    }

    /** 동일한 정상 비밀번호라도 Google 계정의 비밀번호 인증은 거부한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("Google 계정은 저장된 비밀번호가 일치해도 비밀번호 로그인을 거절한다")
    void rejectSocialPassword() {
        Member member = member();
        member.linkSocial("google", "google-123");
        when(repository.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        assertThrows(CredentialsExpiredException.class, () -> authenticationManager.authenticate(credentials()));
    }

    /** 일반 가입 계정의 기존 인증 동작을 유지한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("일반 계정은 정상 비밀번호로 로그인하고 잘못된 비밀번호는 거부한다")
    void localPasswordStillWorks() {
        when(repository.findByEmail("owner@example.com")).thenReturn(Optional.of(member()));
        assertTrue(authenticationManager.authenticate(credentials()).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("owner@example.com", "wrong")));
    }

    /** 소셜 계정의 JWT API 우회 시도에서 토큰 발급이 없는지 확인한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("Google 계정의 JWT 비밀번호 로그인은 401이며 토큰을 만들지 않는다")
    void jwtRejectsSocialPassword() {
        Member member = member();
        member.linkSocial("google", "google-123");
        when(repository.findByEmail("owner@example.com")).thenReturn(Optional.of(member));
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        var response = new JwtLoginController(authenticationManager, tokens)
                .login(new JwtLoginRequest("owner@example.com", "password123!"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(tokens);
    }

    /** 기존 소셜 계정은 제공자의 고유 ID로 다시 찾는다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("기존 Google 회원은 고유 ID로 조회하며 비밀번호 로그인 차단과 별개로 유지한다")
    void existingGoogleMember() {
        Member member = member();
        member.linkSocial("google", "google-123");
        when(repository.findByProviderAndProviderId("google", "google-123")).thenReturn(Optional.of(member));
        assertSame(member, oauth.saveOrUpdate(attributes(), "google"));
        verify(repository, never()).save(any());
    }

    /** 같은 이메일만으로 기존 일반 계정에 Google 인증을 붙이지 않는다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("같은 이메일의 일반 회원을 Google 로그인으로 자동 연결하지 않는다")
    void rejectAutomaticLink() {
        when(repository.existsByNormalizedEmail("owner@example.com")).thenReturn(true);
        var error = assertThrows(OAuth2AuthenticationException.class,
                () -> oauth.saveOrUpdate(attributes(), "google"));
        assertEquals("account_link_required", error.getError().getErrorCode());
        verify(repository, never()).save(any());
    }

    /** 신규 Google 가입 시 로그인 방식 식별 정보를 반드시 기록한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("새 Google 회원은 제공자·고유 ID를 저장하고 비밀번호 인증을 허용하지 않는다")
    void newGoogleMember() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Member saved = oauth.saveOrUpdate(attributes(), "google");
        assertEquals("google", saved.getProvider());
        assertEquals("google-123", saved.getProviderId());
        assertFalse(new CustomUserDetails(saved).isCredentialsNonExpired());
    }

    /** 테스트용 일반 계정에 실제 비밀번호 해시를 설정한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    private Member member() {
        return Member.builder().email("owner@example.com").nickname("테스트")
                .password(encoder.encode("password123!")).role(Role.USER).build();
    }

    /** 동일 입력으로 로그인 방식을 비교한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    private UsernamePasswordAuthenticationToken credentials() {
        return new UsernamePasswordAuthenticationToken("owner@example.com", "password123!");
    }

    /** 외부 Google 요청 없이 확인된 사용자 정보를 재현한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    private OAuthAttributes attributes() {
        return OAuthAttributes.of("google", "sub", Map.of("sub", "google-123",
                "email", "owner@example.com", "name", "테스트"));
    }
}
