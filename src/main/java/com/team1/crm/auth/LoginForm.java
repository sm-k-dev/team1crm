package com.team1.crm.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * + 로그인 입력 폼
 * 역할: 로그인 입력 양식과 메시지 띄우기
 * 작성자: 황현우
 * 일시:2026-11-05
 */

@Getter
@Setter
public class LoginForm { //http://localhost:8082/

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private  String password;
}
