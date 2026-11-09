package com.team1.crm.analytics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 매장의 월간 지표와 요일별·시간대별 방문, 월별 추이를 계산한다.
 *
 * 작성자 sm-k-dev
 * 작성일 2026-11-05
 * 변경노트 2026-11-06 한상희: 매장별 월간 지표 계산과 요일별 차트 데이터 생성 추가.
 * 변경노트 2026-11-09 한상희: 시간대별 방문과 6개월 추이 데이터 추가.
 */
@Service
public class AnalyticsService {
    private final AnalyticsRepository repository;

    /**
     * 방문 집계 조회에 사용할 저장소를 주입받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     *
     * @param repository 분석 조회 저장소
     */
    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    /**
     * 선택한 매장의 월간 방문을 읽기 전용 트랜잭션에서 집계한다.
     * 월 첫날 0시부터 다음 달 첫날 0시 직전까지 포함한다.
     * 기간에 두 번 이상 방문한 고객을 재방문 고객으로 한 번 센다.
     * 매장 접근 권한이 필요한 호출 경로에서는 호출 전에 소유권을 확인해야 한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     * 변경노트 2026-11-06 한상희: 상단 지표와 같은 기간 조건으로 요일별 방문 건수 추가.
     * 변경노트 2026-11-09 한상희: 선택 월 시간대별 집계와 월별 추이를 함께 조회.
     *
     * @param storeId 조회할 매장 ID
     * @param month 조회할 연도와 월
     * @return 월간 요약, 요일별·시간대별 방문 건수, 월별 추이
     */
    @Transactional(readOnly = true)
    public Summary summarize(long storeId, YearMonth month) {
        var start = month.atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();
        var customers = repository.summarizeByCustomer(storeId, start, end);

        long revenue = 0;
        long visits = 0;
        long repeatCustomers = 0;
        for (var customer : customers) {
            revenue += customer.getRevenue();
            visits += customer.getVisitCount();
            if (customer.getVisitCount() >= 2) {
                repeatCustomers++;
            }
        }
        long[] weekdayVisits = new long[7];
        for (var day : repository.countByWeekday(storeId, start, end)) {
            // DB는 일요일이 1이고 화면 배열은 월요일이 0이다. 방문이 없는 요일은 0을 유지한다.
            int mondayBasedIndex = (day.getDayNumber() + 5) % 7;
            weekdayVisits[mondayBasedIndex] = day.getVisitCount();
        }
        long[] hourlyVisits = new long[24];
        for (var hour : repository.countByHour(storeId, start, end)) {
            hourlyVisits[hour.getVisitHour()] = hour.getVisitCount();
        }
        return new Summary(revenue, visits, customers.size(), repeatCustomers, weekdayVisits,
                hourlyVisits, monthlyTrend(storeId, month));
    }

    /**
     * 선택 월을 포함한 6개월의 매출과 방문 고객 수를 오래된 월부터 정리한다.
     * 방문이 없는 월도 0으로 채워 시간 순서가 끊기지 않게 한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     *
     * @param storeId 조회할 매장 ID
     * @param month 추이의 마지막 월
     * @return 월별 지표 6개
     */
    private List<MonthlyPoint> monthlyTrend(long storeId, YearMonth month) {
        MonthlyPoint[] points = new MonthlyPoint[6];
        YearMonth firstMonth = month.minusMonths(points.length - 1);
        for (int index = 0; index < points.length; index++) {
            points[index] = new MonthlyPoint(firstMonth.plusMonths(index).toString(), 0, 0);
        }

        var totals = repository.summarizeByMonth(storeId, firstMonth.atDay(1).atStartOfDay(),
                month.plusMonths(1).atDay(1).atStartOfDay());
        for (var total : totals) {
            YearMonth visitMonth = YearMonth.of(total.getVisitYear(), total.getVisitMonth());
            int index = (int) firstMonth.until(visitMonth, ChronoUnit.MONTHS);
            points[index] = new MonthlyPoint(visitMonth.toString(), total.getRevenue(), total.getCustomerCount());
        }
        return List.of(points);
    }

    /**
     * 월별 추이 차트에 표시할 한 달의 데이터를 담는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     *
     * @param month 연월 표시값
     * @param revenue 결제 금액 합계(원)
     * @param customers 고유 방문 고객 수
     */
    public record MonthlyPoint(String month, long revenue, long customers) {}

    /**
     * 월간 집계 결과를 대시보드에 전달하는 값 객체이다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     * 변경노트 2026-11-06 한상희: 차트에 전달할 요일별 방문 건수 추가.
     * 변경노트 2026-11-09 한상희: 시간대별 방문 건수와 월별 추이 추가.
     *
     * @param revenue 결제 금액 합계(원)
     * @param visits 전체 방문 건수
     * @param customers 한 번 이상 방문한 고유 고객 수
     * @param repeatCustomers 두 번 이상 방문한 고유 고객 수
     * @param weekdayVisits 월요일부터 일요일 순서의 방문 건수 배열
     * @param hourlyVisits 0시부터 23시 순서의 방문 건수 배열
     * @param monthlyTrend 선택 월을 포함한 6개월의 월별 지표
     */
    public record Summary(long revenue, long visits, long customers, long repeatCustomers,
                          long[] weekdayVisits, long[] hourlyVisits, List<MonthlyPoint> monthlyTrend) {
        /**
         * 방문 고객 중 재방문 고객의 비율을 정수 백분율로 반올림한다.
         *
         * 작성자 한상희
         * 작성일 2026-11-06
         *
         * @return 재방문율. 방문 고객이 없으면 null이다.
         */
        public Long repeatRatePercent() {
            return customers == 0 ? null : Math.round(100.0 * repeatCustomers / customers);
        }
    }
}
