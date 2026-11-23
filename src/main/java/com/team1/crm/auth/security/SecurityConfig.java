
package com.team1.crm.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import jakarta.servlet.DispatcherType;

/**
 * + Spring Security 보안 및 인증 설정
 * 역할: 회원 로그인, Google OAuth2 로그인, JWT 인증,
 *      URL별 접근 권한, 로그아웃 등 전체 보안 기능을 설정합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */


@Configuration // 스프링 설정 클래스 등록
@EnableWebSecurity // Spring Security 웹 보안 기능 활성화
@RequiredArgsConstructor // final 필드의 생성자를 자동 생성
public class SecurityConfig {

    // Google OAuth2 로그인 사용자 정보 처리 서비스
    private final CustomOAuth2UserService customOAuth2UserService;
    // JWT 토큰 생성 및 검증 담당
    private final JwtTokenProvider jwtTokenProvider;
    // DB에서 회원 정보를 조회하는 인증 서비스
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * + Bearer JWT API 전용 보안 설정
     * 역할: Bearer 토큰을 사용하는 API 요청을 일반 웹 세션과 분리하여 인증한다.
     *       웹 CSRF 보호는 유지하면서 JWT 전용 요청에는 CSRF 검사를 적용하지 않는다.
     *
     * 작성자: 황현우
     * 일시: 2026-11-16
     */
    @Bean
    @Order(1)
    public SecurityFilterChain jwtApiFilterChain(HttpSecurity http)
            throws Exception {

        http
                // 1. Bearer 헤더가 있는 비공개 API 요청만 JWT 체인으로 분리
                .securityMatcher(request -> {
                    String path = request.getServletPath();
                    String authorization = request.getHeader("Authorization");

                    boolean publicAuthApi =
                            path.equals("/api/members/login")
                                    || path.equals("/api/members/signup")
                                    || path.equals("/api/members/check-email")
                                    || path.equals("/api/members/check-nickname");

                    return path.startsWith("/api/")
                            && !publicAuthApi
                            && authorization != null
                            && authorization.startsWith("Bearer ")
                            && authorization.length() > 7;
                })

                // 2. JWT 전용 요청에만 CSRF 검사 제외
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 기존 웹 로그인 세션을 인증에 사용하지 않음
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. JWT로 인증된 회원만 접근 허용
                .authorizeHttpRequests(auth -> auth

                        // 관리자 전용 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 그 외 JWT API는 인증된 회원만 접근
                        .anyRequest().authenticated()
                )

                // 5. 미인증 요청은 로그인 화면이 아니라 401 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )

                // 6. 기존 JWT 인증 필터 사용
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider,
                                customUserDetailsService
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
    /**
     * + Spring Security 필터 체인 설정
     * 역할: HTTP 요청에 대한 보안 정책과 로그인 및 인증 방식을 설정합니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 보호 설정 (JWT 로그인 API는 검사 제외)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/members/login")
                )

                // 세션이 필요한 경우에만 생성
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // [2026-11-16 / 황현우]
                        // 오류 처리 요청(ERROR)은 로그인 화면으로 보내지 않고
                        // 원래 HTTP 오류 상태를 반환할 수 있도록 허용
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // 로그인 없이 접근 가능한 페이지 및 리소스
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/forgot-password",  // [2026-11-16 / 황현우] 비로그인 회원도 비밀번호 찾기·재설정 이용 가능
                                "/reset-password",
                                "/api/members/signup",
                                "/api/members/check-email",
                                "/api/members/check-nickname", // [2026-11-16 / 황현우] 회원가입 전 닉네임 중복확인 API 공개
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/members/login"
                        ).permitAll()
                        // 관리자 전용 페이지 및 API
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 그 외 페이지는 로그인 필요
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .defaultSuccessUrl("/", true)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider,
                                customUserDetailsService
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
