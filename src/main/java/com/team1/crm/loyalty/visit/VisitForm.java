package com.team1.crm.loyalty.visit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// 방문 등록 폼. 명세(docs/architecture/05_SaaS_API.md): POST /customers/{id}/visits
// 쿠폰 사용(couponId)과 포인트 사용(usedPoints)도 이 폼 하나에서 함께 처리한다 (2026-11-16 결정).
// 필드명은 templates/loyalty/_visit.html 의 input name 과 맞춘다.
public class VisitForm {

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String requestKey; // 같은 방문의 네트워크 재시도에는 같은 UUID, 새 방문에는 새 UUID

    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }

    @NotNull
    @Min(0)
    @Max(100_000_000)
    private Integer amount;          // 결제 금액(원)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // HTML datetime-local 값("2026-11-16T12:30")을 받기 위함
    private LocalDateTime visitedAt; // 방문 일시. 비어 있으면 서버 현재 시각으로 채운다

    @Size(max = 200)
    private String menuMemo;         // 방문 메모 (엔티티 Visit.menuMemo 와 같은 이름)

    private Long couponId;           // 이번 결제에 사용할 쿠폰 ID. 비어 있으면 쿠폰 없이 등록한다

    @Min(0)
    private Integer usedPoints;      // 이번 결제에 사용할 포인트 (1P = 1원). 비어 있으면 0

    public Integer getUsedPoints() { return usedPoints; }
    public void setUsedPoints(Integer usedPoints) { this.usedPoints = usedPoints; }

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }

    public String getMenuMemo() { return menuMemo; }
    public void setMenuMemo(String menuMemo) { this.menuMemo = menuMemo; }
}
