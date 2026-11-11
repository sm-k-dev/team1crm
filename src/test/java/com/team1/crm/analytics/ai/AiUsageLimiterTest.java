package com.team1.crm.analytics.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 문구 생성 실패 후 일일 제한이 복구되는지 확인한다. 작성자 한상희, 작성일 2026-11-11 */
class AiUsageLimiterTest {
    /** 실패한 예약은 횟수를 돌려주고 완료된 다섯 건은 제한한다. 작성자 한상희, 작성일 2026-11-11 */
    @Test
    void failedRequestsDoNotConsumeDailyLimit() {
        var limiter = new AiUsageLimiter();
        long memberId = 1L;

        for (int attempt = 0; attempt < 6; attempt++) {
            var reservation = limiter.reserve(memberId);
            assertEquals(4, reservation.remaining());
            limiter.release(reservation);
        }
        for (int remaining = 4; remaining >= 0; remaining--) {
            assertEquals(remaining, limiter.reserve(memberId).remaining());
        }
        var exception = assertThrows(ResponseStatusException.class, () -> limiter.reserve(memberId));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }
}
