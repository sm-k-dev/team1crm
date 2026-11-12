package com.team1.crm.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 소유자 ID를 입력받지 않는 내 매장 수정 폼이다. 작성자: 김순민 / 작성일: 2026-11-12 */
@Getter @Setter
public class MyStoreForm {
    @NotBlank(message = "매장명을 입력해 주세요.")
    @Size(max = 50, message = "매장명은 50자 이내로 입력해 주세요.")
    private String name;
    @Size(max = 30, message = "업종은 30자 이내로 입력해 주세요.")
    private String category;
    @Size(max = 200, message = "주소는 200자 이내로 입력해 주세요.")
    private String address;
    @NotNull(message = "적립률을 입력해 주세요.")
    @Min(value = 0, message = "적립률은 0~20%로 입력해 주세요.")
    @Max(value = 20, message = "적립률은 0~20%로 입력해 주세요.")
    private Integer pointRate;
}
