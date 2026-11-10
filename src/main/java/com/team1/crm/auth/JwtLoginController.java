
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class JwtLoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // JWT 로그인 API
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody JwtLoginRequest request
    ) {

        if (request.email() == null ||
                request.email().isBlank() ||
                request.password() == null ||
                request.password().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "이메일과 비밀번호를 입력해주세요."
                    ));
        }

        try {

            // 1. 이메일과 비밀번호 인증
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.email(),
                                    request.password()
                            )
                    );

            // 2. 인증 성공 시 JWT 생성
            String token = jwtTokenProvider.generateToken(authentication);

            // 3. JWT를 JSON으로 반환
            return ResponseEntity.ok(Map.of(
                    "accessToken", token,
                    "tokenType", "Bearer"
            ));

        } catch (BadCredentialsException e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "이메일 또는 비밀번호가 올바르지 않습니다."
                    ));
        }
    }
}
