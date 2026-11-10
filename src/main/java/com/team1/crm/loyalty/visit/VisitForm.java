package com.team1.crm.loyalty.visit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 방문 등록 폼. 필드명은 templates/loyalty/_visit.html 의 input name 과 맞춘다.
public class VisitForm {

    @NotNull
    @Min(0)
    @Max(100_000_000)
    private Integer amount;          // 결제 금액(원)

    @Min(0)
    private Integer usedPoints = 0;  // 이번 결제에 사용할 포인트 (서버에서 잔액 검증)

    @Size(max = 200)
    private String memo;             // 방문 메모

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getUsedPoints() { return usedPoints; }
    public void setUsedPoints(Integer usedPoints) { this.usedPoints = usedPoints; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
