package com.team1.crm.loyalty.visit;

// 방문 등록 응답. 명세: visitId, earnedPoints
public record VisitResult(Long visitId, int earnedPoints) {
}
