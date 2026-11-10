package com.team1.crm.analytics;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;

/**
 * 개발 프로필에서 로그인한 회원의 소유 매장에 대한 월간 분석 화면을 제공한다.
 *
 * 작성자 sm-k-dev
 * 작성일 2026-11-05
 * 변경노트 2026-11-06 한상희: 월 선택과 테스트 매장 집계를 기존 화면에 연결.
 * 변경노트 2026-11-10 한상희: 고객별 RFM 표시 전에 로그인 회원의 매장 소유권 확인.
 */
@Controller
@Profile("dev")
public class DashboardViewController {
    private final AnalyticsService analyticsService;
    private final StoreService storeService;

    /**
     * 분석 집계 서비스와 로그인 회원의 매장 조회 서비스를 주입받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     * 변경노트 2026-11-10 한상희: 고정 테스트 매장 설정을 소유 매장 조회 서비스로 교체.
     *
     * @param analyticsService 월간 방문 지표 계산 서비스
     * @param storeService 회원의 소유 매장 조회 서비스
     */
    public DashboardViewController(AnalyticsService analyticsService,
                                   StoreService storeService) {
        this.analyticsService = analyticsService;
        this.storeService = storeService;
    }

    /**
     * 선택한 월의 분석 결과를 화면 모델에 담는다.
     * 조회 월을 생략하면 서울 기준 현재 월을 사용한다.
     * 매장을 생략하면 소유 매장 중 ID가 가장 작은 매장을 선택한다.
     *
     * 작성자 sm-k-dev
     * 작성일 2026-11-05
     * 변경노트 2026-11-06 한상희: 월 입력을 받고 집계 결과와 재방문율을 화면 모델에 전달.
     * 변경노트 2026-11-10 한상희: 소유 매장 선택 및 RFM 결과 전달 추가.
     *
     * @param month 조회할 연도와 월. 생략할 수 있다.
     * @param storeId 선택 매장 ID. 생략할 수 있다.
     * @param member 로그인한 회원
     * @param model 템플릿에 값을 전달할 화면 모델
     * @return 매장 분석 템플릿 이름
     * @throws ResponseStatusException 로그인 회원을 확인할 수 없거나 타 매장에 접근할 때
     */
    @GetMapping("/analytics/dashboard")
    public String dashboard(@RequestParam(name = "month", required = false)
                            @DateTimeFormat(pattern = "uuuu-MM") YearMonth month,
                            @RequestParam(name = "storeId", required = false) Long storeId,
                            @LoginMember Member member, Model model) {
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        YearMonth selectedMonth = month == null ? YearMonth.now(ZoneId.of("Asia/Seoul")) : month;
        model.addAttribute("selectedMonth", selectedMonth.toString());
        var stores = storeService.getMyStores(member.getEmail()).stream()
                .sorted(Comparator.comparing(Store::getId)).toList();
        model.addAttribute("stores", stores);
        if (stores.isEmpty() && storeId == null) {
            return "analytics/dashboard";
        }

        Store selectedStore = storeId == null ? stores.getFirst() : stores.stream()
                .filter(store -> store.getId().equals(storeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "매장 조회 권한이 없습니다."));
        var summary = analyticsService.summarize(selectedStore.getId(), selectedMonth);
        model.addAttribute("summary", summary);
        model.addAttribute("repeatRate", summary.repeatRatePercent());
        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("rfm", analyticsService.summarizeRfm(selectedStore.getId(), selectedMonth));
        return "analytics/dashboard";
    }
}
