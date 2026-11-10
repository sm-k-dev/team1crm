package com.team1.crm.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * + 회원가입 입력 폼
 * 역할: 회원가입 입력 양식과 메시지 띄우기
 *
 * 작성자: 황현우
 * 일시:2026-11-05
 */


@Getter
@Setter
public class MemberSignupForm {

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    private String nickname;

    // 회원 가입시 선택할 수 있는 권한 (기본값은 서비스나 컨트롤러에서 처리)
    private  Role role;


}
