package com.team1.crm.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 클래스
 * 역할: HTTP 요청 헤더에서 JWT 토큰을 추출하고 유효성 검증 후 SecurityContext에 인증 정보 저장
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사
        if (token != null
                && jwtTokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 3. 토큰에서 회원 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(token);

            // 4. UserDetailsService를 통해 UserDetails 조회
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 5. Spring Security 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 6. SecurityContext에 인증 객체 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 Bearer 토큰 문자열 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}