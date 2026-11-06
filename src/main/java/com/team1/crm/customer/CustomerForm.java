package com.team1.crm.customer;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 고객 등록·수정 입력값을 전달받고 검증하는 DTO 클래스이다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 */
@Getter
@Setter
public class CustomerForm {

    @NotBlank(message = "고객 이름을 입력해 주세요.")
    @Size(max = 30, message = "고객 이름은 30자 이내로 입력해 주세요.")
    private String name;

    @NotBlank(message = "고객 전화번호를 입력해 주세요.")
    @Pattern(
            regexp = "^010[0-9]{8}$",
            message = "전화번호는 010으로 시작하는 숫자 11자리여야 합니다."
    )
    private String phone;

    @Min(value = 1, message = "생일 월은 1월부터 12월까지 선택해 주세요.")
    @Max(value = 12, message = "생일 월은 1월부터 12월까지 선택해 주세요.")
    private Integer birthMonth;

    @Size(max = 300, message = "메모는 300자 이내로 입력해 주세요.")
    private String memo;

    private boolean agreeMarketing;
}
