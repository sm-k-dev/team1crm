package com.team1.crm.analytics.ai;

import com.team1.crm.analytics.AnalyticsService;
import com.team1.crm.auth.Member;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

/**
 * 소유 매장의 업종과 RFM 등급별 집계로 재방문 문구를 생성하는 API.
 *
 * 작성자 한상희
 * 작성일 2026-11-11
 */
@RestController
public class AiApiController {
    private final StoreService stores;
    private final AnalyticsService analytics;
    private final AiMarketingService marketing;
    private final AiUsageLimiter usage;

    /** 매장 조회, 분석, 문구 생성 및 사용량 제한 서비스를 받는다. 작성자 한상희, 작성일 2026-11-11 */
    public AiApiController(StoreService stores, AnalyticsService analytics,
                           AiMarketingService marketing, AiUsageLimiter usage) {
        this.stores = stores;
        this.analytics = analytics;
        this.marketing = marketing;
        this.usage = usage;
    }

    /**
     * 매장 소유권을 확인하고 저장된 업종과 서버가 계산한 등급별 고객 수를 사용한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-11
     * 변경노트 2026-11-11 한상희: 문구 생성에 실패하면 예약한 횟수를 돌려준다.
     */
    @PostMapping("/api/ai/marketing-suggestions")
    public Suggestions suggest(@LoginMember Member member,
                               @RequestParam("storeId") long storeId,
                               @RequestParam("month") @DateTimeFormat(pattern = "uuuu-MM") YearMonth month,
                               @RequestParam("grade") String grade) {
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var store = stores.getMyStores(member.getEmail()).stream()
                .filter(ownedStore -> ownedStore.getId().equals(storeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var segment = analytics.summarizeRfm(storeId, month).gradeCounts().stream()
                .filter(count -> count.label().equals(grade) && count.count() > 0)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        var reservation = usage.reserve(store.getId());
        try {
            var result = marketing.generate(segment, store.getCategory(), reservation.remaining());
            return new Suggestions(result.messages(), reservation.remaining(), result.example());
        } catch (RuntimeException exception) {
            usage.release(reservation);
            throw exception;
        }
    }

    /** 생성 문구와 남은 일일 횟수를 전달한다. 작성자 한상희, 작성일 2026-11-11 */
    public record Suggestions(List<String> suggestions, int remaining, boolean example) {}

    /** 외부 AI 오류에 대해 화면용 안내 문구를 전달한다. 작성자 한상희, 작성일 2026-11-11 */
    @ExceptionHandler(AiUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public AiError handleAiUnavailable(AiUnavailableException exception) {
        return new AiError(exception.getMessage());
    }

    /** 화면에 표시할 오류 안내를 담는다. 작성자 한상희, 작성일 2026-11-11 */
    public record AiError(String message) {}
}
