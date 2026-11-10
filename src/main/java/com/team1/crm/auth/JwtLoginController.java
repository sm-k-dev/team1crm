
package com.team1.crm.auth;

import com.team1.crm.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * + JWT 로그인 API 컨트롤러
 * 역할: 사용자가 입력한 이메일과 비밀번호를 검증하고,
 *      인증에 성공하면 JWT 토큰을 발급하여 JSON으로 반환합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */

@RestController // HTTP 요청을 처리하고 결과를 JSON으로 반환
@RequiredArgsConstructor // final 필드를 이용한 생성자 자동 생성
@RequestMapping("/api/members") // 회원 관련 API의 공통 주소
public class JwtLoginController {

    // 사용자 이메일과 비밀번호를 검증하는 인증 관리자
    private final AuthenticationManager authenticationManager;
    // 인증 성공 후 JWT 토큰을 생성하는 클래스
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * + JWT 로그인 처리
     * 역할: 로그인 요청값을 검사하고 사용자 인증을 진행합니다.
     *      인증 성공 시 JWT를 반환하고, 실패 시 오류 메시지를 반환합니다.
     */
    @PostMapping("/login") // POST /api/members/login
    public ResponseEntity<?> login(
            @RequestBody JwtLoginRequest request // JSON 요청 데이터를 객체로 변환
    ) {

        // 이메일 또는 비밀번호가 입력되지 않았는지 확인
        if (request.email() == null ||
                request.email().isBlank() ||
                request.password() == null ||
                request.password().isBlank()) {

            // 필수 입력값이 없으면 400 Bad Request 반환
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "이메일과 비밀번호를 입력해주세요."
                    ));
        }

        try {

            // 1. 입력받은 이메일과 비밀번호로 인증 객체 생성 및 검증
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.email(),
                                    request.password()
                            )
                    );

            // 2. 인증에 성공한 사용자 정보를 기반으로 JWT 토큰 생성
            String token = jwtTokenProvider.generateToken(authentication);

            // 3. 생성된 JWT를 JSON 형식으로 반환 (200 OK)
            return ResponseEntity.ok(Map.of(
                    "accessToken", token,
                    "tokenType", "Bearer"
            ));

        } catch (BadCredentialsException e) {

            // 이메일 또는 비밀번호가 일치하지 않으면 401 Unauthorized 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "이메일 또는 비밀번호가 올바르지 않습니다."
                    ));
        }
    }
}
