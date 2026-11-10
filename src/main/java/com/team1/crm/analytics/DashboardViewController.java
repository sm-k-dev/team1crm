package com.team1.crm.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 개발 프로필에서 설정으로 지정한 테스트 매장의 월간 분석 화면을 제공한다.
 * 이 미리보기는 로그인한 점주의 매장 소유권을 확인하지 않는다.
 *
 * 작성자 sm-k-dev
 * 작성일 2026-11-05
 * 변경노트 2026-11-06 한상희: 월 선택과 테스트 매장 집계를 기존 화면에 연결.
 */
@Controller
@Profile("dev")
public class DashboardViewController {
    private final AnalyticsService analyticsService;
    private final long previewStoreId;

    /**
     * 집계 서비스와 서버 설정에 지정한 테스트 매장 ID를 주입받는다.
     * 매장 설정을 생략하면 기본값 0을 사용해 DB 집계를 건너뛴다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     *
     * @param analyticsService 월간 방문 지표 계산 서비스
     * @param previewStoreId 개발용 테스트 매장 ID
     */
    public DashboardViewController(AnalyticsService analyticsService,
                                   @Value("${app.analytics.preview-store-id:0}") long previewStoreId) {
        this.analyticsService = analyticsService;
        this.previewStoreId = previewStoreId;
    }

    /**
     * 선택한 월의 분석 결과를 화면 모델에 담는다.
     * 조회 월을 생략하면 서울 기준 현재 월을 사용한다.
     * 조회할 매장은 URL 입력과 별개로 서버 실행 설정에서 결정한다.
     *
     * 작성자 sm-k-dev
     * 작성일 2026-11-05
     * 변경노트 2026-11-06 한상희: 월 입력을 받고 집계 결과와 재방문율을 화면 모델에 전달.
     *
     * @param month 조회할 연도와 월. 생략할 수 있다.
     * @param model 템플릿에 값을 전달할 화면 모델
     * @return 매장 분석 템플릿 이름
     */
    @GetMapping("/analytics/dashboard")
    public String dashboard(@RequestParam(name = "month", required = false)
                            @DateTimeFormat(pattern = "uuuu-MM") YearMonth month, Model model) {
        YearMonth selectedMonth = month == null ? YearMonth.now(ZoneId.of("Asia/Seoul")) : month;
        model.addAttribute("selectedMonth", selectedMonth.toString());
        if (previewStoreId > 0) {
            var summary = analyticsService.summarize(previewStoreId, selectedMonth);
            model.addAttribute("summary", summary);
            model.addAttribute("repeatRate", summary.repeatRatePercent());
            model.addAttribute("previewStoreId", previewStoreId);
        }
        return "analytics/dashboard";
    }
}
