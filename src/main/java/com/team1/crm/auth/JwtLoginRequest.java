
package com.team1.crm.auth;

/**
 * JWT 로그인 요청 DTO
 * 클라이언트가 전달하는 이메일과 비밀번호를 받는다.
 */
public record JwtLoginRequest(
        String email,
        String password
) {
}
