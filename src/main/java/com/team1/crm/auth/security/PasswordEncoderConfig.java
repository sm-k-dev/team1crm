package com.team1.crm.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * + Argon2 비밀번호 암호화 빈 설정
 * 역할: 보안 설정
 * 작성자: 황현우
 * 일시:2026-11-05
 */

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        // Spring Security 권장 기본 설정값(v5.8 기준)을 사용하는 Argon2 인코더 등록
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    }
}
