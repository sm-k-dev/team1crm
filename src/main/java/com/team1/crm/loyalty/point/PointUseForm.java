package com.team1.crm.loyalty.point;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 포인트 사용 요청. 명세: POST /api/customers/{id}/points/use → points
public class PointUseForm {

    @NotNull
    @Min(1)
    private Integer points;

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}
