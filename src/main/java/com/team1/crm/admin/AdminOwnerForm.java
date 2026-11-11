package com.team1.crm.admin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 관리자용 사장님 계정 등록 입력이다. 권한은 입력받지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Getter
@Setter
public class AdminOwnerForm {
    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일을 입력해 주세요.")
    @Size(max = 255, message = "이메일은 255자 이내로 입력해 주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 255, message = "이름은 255자 이내로 입력해 주세요.")
    private String nickname;

    @NotBlank(message = "초기 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 128, message = "초기 비밀번호는 8~128자로 입력해 주세요.")
    private String password;
}
