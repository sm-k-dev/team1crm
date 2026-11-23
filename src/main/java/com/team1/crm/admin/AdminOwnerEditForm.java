package com.team1.crm.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 사장님 이름 수정 전용 입력이다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Getter
@Setter
public class AdminOwnerEditForm {
    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 255, message = "이름은 255자 이내로 입력해 주세요.")
    private String nickname;
}
