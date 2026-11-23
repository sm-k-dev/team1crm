package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * 소셜 로그인 성공 핸들러
 * 역할: 소셜 로그인 성공 시 JWT 토큰을 생성하고 클라이언트에 전달(리다이렉트)
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 구글 등에서 제공하는 이메일 정보 추출
        String email = (String) attributes.get("email");

        // DB에서 회원을 조회하여 인증 객체 구성 또는 토큰 발급용 정보 활용
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 존재하지 않습니다. 이메일: " + email));

        // JWT 토큰 생성을 위해 필요한 Authentication 객체 생성 혹은 직접 토큰 생성 로직 연동
        // 여기서는 간단하게 회원 정보를 바탕으로 토큰 생성 기능을 호출하거나 인증 처리를 위임할 수 있습니다.
        // (필요에 따라 Authentication 객체를 새로 구성하여 jwtTokenProvider.generateToken 전달)
        String token = jwtTokenProvider.generateToken(authentication);

        // 토큰을 포함하여 리다이렉트할 프론트엔드 또는 메인 페이지 URL 설정
        // 예: 프론트엔드로 토큰을 쿼리 파라미터로 넘겨주거나 쿠키에 저장
        String targetUrl = UriComponentsBuilder.fromUriString("/home") // 이동할 페이지 경로
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}