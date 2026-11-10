package com.team1.crm.auth.security;

import java.lang.annotation.*;

/**
 * 커스텀 로그인 회원 정보 주입 어노테이션
 * 역할: 컨트롤러 메서드 파라미터에서 현재 로그인한 회원 정보를 바로 받아올 수 있도록 지원
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Target(ElementType.PARAMETER) // 메서드의 파라미터에만 붙일 수 있도록 설정
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 어노테이션 정보 유지
public @interface LoginMember {
}