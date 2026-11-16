package com.team1.crm.loyalty.visit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 적립 포인트 계산 규칙을 DB 없이 검증한다.
 * 규칙(2026-11-16 확정): 결제 금액 × 적립률 / 100, 소수점 첫째 자리에서 반올림.
 *
 * 작성자: 이재준
 */
class VisitServiceTest {

    @ParameterizedTest(name = "{0}원, 적립률 {1}% → {2}P")
    @CsvSource({
            "10000, 5, 500",
            "2950, 1, 30",      // 29.5 → 반올림 30
            "2949, 1, 29",      // 29.49 → 29
            "50, 1, 1",         // 0.5 → 1 (반올림 경계)
            "49, 1, 0",         // 0.49 → 0
            "0, 5, 0",
            "10000, 0, 0",      // 적립률 0%
            "100000000, 20, 20000000",
            "2147483647, 20, 429496729"  // int 최대값 × 20이 int 범위를 넘어도 long으로 계산해 오버플로 없음
    })
    @DisplayName("적립액은 금액 × 적립률 / 100을 소수점 첫째 자리에서 반올림한다")
    void calculatesEarnedPoints(int amount, int rate, int expected) {
        assertEquals(expected, VisitService.calculateEarnedPoints(amount, rate));
    }
}
