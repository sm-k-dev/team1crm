package com.team1.crm.loyalty.visit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// 방문 등록 폼. 명세(docs/architecture/05_SaaS_API.md): POST /customers/{id}/visits → amount, visitedAt
// 포인트 사용은 별도 API(/points/use)라서 이 폼에는 포함하지 않는다.
// 필드명은 templates/loyalty/_visit.html 의 input name 과 맞춘다.
public class VisitForm {

    @NotNull
    @Min(0)
    @Max(100_000_000)
    private Integer amount;          // 결제 금액(원)

    private LocalDateTime visitedAt; // 방문 일시. 비어 있으면 서버 현재 시각으로 채운다

    @Size(max = 200)
    private String menuMemo;         // 방문 메모 (엔티티 Visit.menuMemo 와 같은 이름)

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }

    public String getMenuMemo() { return menuMemo; }
    public void setMenuMemo(String menuMemo) { this.menuMemo = menuMemo; }
}
