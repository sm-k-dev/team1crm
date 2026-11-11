package com.team1.crm.admin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 관리자 매장 등록·수정 입력값이다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Getter
@Setter
public class AdminStoreForm {
    @NotBlank(message = "매장명을 입력해 주세요.")
    @Size(max = 50, message = "매장명은 50자 이내로 입력해 주세요.")
    private String name;

    @Size(max = 30, message = "업종은 30자 이내로 입력해 주세요.")
    private String category;

    @Size(max = 200, message = "주소는 200자 이내로 입력해 주세요.")
    private String address;

    @NotNull(message = "사장님을 선택해 주세요.")
    @Positive(message = "올바른 사장님을 선택해 주세요.")
    private Long ownerId;

    @NotNull(message = "포인트 적립률을 입력해 주세요.")
    @Min(value = 0, message = "적립률은 0~100 사이로 입력해 주세요.")
    @Max(value = 100, message = "적립률은 0~100 사이로 입력해 주세요.")
    private Integer pointRate = 5;
}
