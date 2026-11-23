package com.team1.crm.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 로그인 회원의 이름 수정 입력이다. 작성자: 김순민 / 작성일: 2026-11-12 */
@Getter @Setter
public class MyProfileForm {
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 255, message = "이름은 255자 이내로 입력해 주세요.")
    private String nickname;
}
