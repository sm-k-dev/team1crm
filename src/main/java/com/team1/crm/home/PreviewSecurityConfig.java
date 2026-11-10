
package com.team1.crm.home;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 개발 환경에서 정적 리소스만 별도로 허용하는 설정
 *
 * 로그인, 회원가입, 로그아웃 및 실제 서비스 페이지는
 * SecurityConfig에서 처리하도록 합니다.
 *
 *
 *   개발용 화면 미리보기 설정.
 *
 *  실제 인증과 접근 제어는 SecurityConfig에서 처리합니다.
 *  별도의 SecurityFilterChain을 등록하지 않습니다.
 *
 */
@Configuration
@Profile("dev")
public class PreviewSecurityConfig {
}