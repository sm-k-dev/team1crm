package com.team1.crm.loyalty.point;

// 포인트 사용 응답. 명세: balance, historyId
public record PointUseResult(long balance, Long historyId) {
}
