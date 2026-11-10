package com.team1.crm.loyalty.coupon;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 쿠폰 발급 폼. 필드명은 templates/loyalty/_coupon.html 의 input name 과 맞춘다.
public class CouponForm {

    @NotBlank
    @Size(max = 50)
    private String title;

    @NotNull
    @Min(1)
    private Integer discount;      // 할인 금액(원)

    @NotNull
    private LocalDate expiresAt;   // 만료일

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDiscount() { return discount; }
    public void setDiscount(Integer discount) { this.discount = discount; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
