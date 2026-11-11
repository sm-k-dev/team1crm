package com.team1.crm.analytics.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 서버 인스턴스에서 회원별 일일 문구 생성 횟수를 관리한다.
 *
 * 작성자 한상희
 * 작성일 2026-11-11
 */
@Component
public class AiUsageLimiter {
    private static final int DAILY_LIMIT = 5;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private LocalDate usageDate = LocalDate.now(SEOUL);
    private final Map<Long, Integer> requests = new HashMap<>();

    /**
     * 서울 날짜를 기준으로 한 자리를 예약해 동시 요청이 한도를 넘지 않게 한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-11
     * 변경노트 2026-11-11 한상희: 실패한 요청의 예약을 취소할 수 있도록 날짜를 함께 반환한다.
     */
    public synchronized Reservation reserve(long memberId) {
        LocalDate today = LocalDate.now(SEOUL);
        if (!today.equals(usageDate)) {
            requests.clear();
            usageDate = today;
        }
        int used = requests.getOrDefault(memberId, 0);
        if (used >= DAILY_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        requests.put(memberId, used + 1);
        return new Reservation(memberId, today, DAILY_LIMIT - used - 1);
    }

    /**
     * 실패한 요청의 자리를 돌려준다. 날짜가 바뀌어 새 집계가 시작됐으면 이전 예약은 무시한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-11
     */
    public synchronized void release(Reservation reservation) {
        if (usageDate.equals(reservation.date())) {
            requests.computeIfPresent(reservation.memberId(), (id, used) -> used == 1 ? null : used - 1);
        }
    }

    /** 예약한 회원, 서울 날짜, 예약 직후 남은 횟수를 담는다. 작성자 한상희, 작성일 2026-11-11 */
    public record Reservation(long memberId, LocalDate date, int remaining) {}
}
