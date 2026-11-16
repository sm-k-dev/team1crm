package com.team1.crm.loyalty.visit;

/**
 * 방문 등록 결과.
 *
 * @param visitId        저장된 방문 ID
 * @param earnedPoints   이번에 적립된 포인트 (쿠폰·포인트를 쓴 뒤의 결제 금액 기준)
 * @param discountAmount 쿠폰으로 할인된 금액 (쿠폰을 쓰지 않았으면 0)
 * @param usedPoints     이번 결제에 사용한 포인트 (1P = 1원, 쓰지 않았으면 0)
 * @param paidAmount     쿠폰과 포인트를 모두 뺀 실제 결제 금액 (방문 기록의 금액과 같다)
 */
public record VisitResult(Long visitId, int earnedPoints, int discountAmount, int usedPoints, int paidAmount) {}
