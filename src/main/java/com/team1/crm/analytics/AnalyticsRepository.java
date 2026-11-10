package com.team1.crm.analytics;

import com.team1.crm.loyalty.visit.Visit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 지정한 매장과 기간의 매출과 방문 기록을 집계하는 조회 인터페이스이다.
 *
 * 작성자 sm-k-dev
 * 작성일 2026-11-05
 * 변경노트 2026-11-06 한상희: 대시보드에 필요한 고객별 합계와 요일별 방문 건수 조회 추가.
 * 변경노트 2026-11-09 한상희: 시간대별 방문과 월별 매출·방문 고객 조회 추가.
 * 변경노트 2026-11-10 한상희: 매장과 기간을 제한한 고객별 RFM 점수 조회 추가.
 */
public interface AnalyticsRepository extends Repository<Visit, Long> {
    /**
     * 조회 기간에 방문한 고객마다 결제 합계와 방문 건수를 한 행으로 반환한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     *
     * @param storeId 조회할 매장 ID
     * @param start 조회 시작 시각. 경계값을 포함한다.
     * @param end 조회 종료 시각. 경계값을 제외한다.
     * @return 고객별 집계 목록. 해당 기간의 방문이 없으면 빈 목록이다.
     */
    @Query("""
            select sum(v.amount) as revenue, count(v.id) as visitCount
            from Visit v
            where v.customer.store.id = :storeId
              and v.visitedAt >= :start
              and v.visitedAt < :end
            group by v.customer.id
            """)
    List<CustomerTotals> summarizeByCustomer(@Param("storeId") long storeId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /**
     * 매장과 기간을 기준으로 방문 기록을 요일별로 센다.
     * MySQL의 DAYOFWEEK는 일요일을 1, 토요일을 7로 반환한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     *
     * @param storeId 조회할 매장 ID
     * @param start 조회 시작 시각. 경계값을 포함한다.
     * @param end 조회 종료 시각. 경계값을 제외한다.
     * @return 방문 기록이 있는 요일의 집계 목록
     */
    @Query(value = """
            select dayofweek(v.visited_at) as dayNumber, count(*) as visitCount
            from visit v
            join customer c on c.id = v.customer_id
            where c.store_id = :storeId
              and v.visited_at >= :start
              and v.visited_at < :end
            group by dayofweek(v.visited_at)
            """, nativeQuery = true)
    List<WeekdayTotal> countByWeekday(@Param("storeId") long storeId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /**
     * 저장된 방문 시각의 시간 부분을 기준으로 방문 건수를 센다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     *
     * @param storeId 조회할 매장 ID
     * @param start 포함하는 시작 시각
     * @param end 제외하는 종료 시각
     * @return 방문 기록이 있는 시간대의 집계 목록
     */
    @Query(value = """
            select hour(v.visited_at) as visitHour, count(*) as visitCount
            from visit v
            join customer c on c.id = v.customer_id
            where c.store_id = :storeId
              and v.visited_at >= :start
              and v.visited_at < :end
            group by hour(v.visited_at)
            """, nativeQuery = true)
    List<HourlyTotal> countByHour(@Param("storeId") long storeId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    /**
     * 연도와 월마다 결제 합계와 방문한 고유 고객 수를 구한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     *
     * @param storeId 조회할 매장 ID
     * @param start 포함하는 시작 시각
     * @param end 제외하는 종료 시각
     * @return 방문 기록이 있는 월의 집계 목록
     */
    @Query(value = """
            select year(v.visited_at) as visitYear, month(v.visited_at) as visitMonth,
                   sum(v.amount) as revenue, count(distinct v.customer_id) as customerCount
            from visit v
            join customer c on c.id = v.customer_id
            where c.store_id = :storeId
              and v.visited_at >= :start
              and v.visited_at < :end
            group by year(v.visited_at), month(v.visited_at)
            """, nativeQuery = true)
    List<MonthlyTotal> summarizeByMonth(@Param("storeId") long storeId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * 방문 고객의 최근성·빈도·금액을 각각 NTILE(5)로 점수화한다.
     * 높은 값부터 나누어 가장 좋은 구간에 5점을 부여하고, 동점 순서는 고객 ID로 고정한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-10
     *
     * @param storeId 호출부에서 소유권을 검증한 매장 ID
     * @param start 포함하는 시작 시각
     * @param end 제외하는 종료 시각
     * @return 선택 기간에 방문한 고객별 집계와 점수
     */
    @Query(value = """
            with customer_visits as (
                select c.id as customerId, c.name as customerName, max(v.visited_at) as lastVisit,
                       count(*) as visitCount, sum(v.amount) as totalAmount
                from visit v
                join customer c on c.id = v.customer_id
                where c.store_id = :storeId
                  and v.visited_at >= :start
                  and v.visited_at < :end
                group by c.id, c.name
            )
            select customerName, cast(cast(lastVisit as date) as char(10)) as lastVisitDate,
                   visitCount, totalAmount,
                   6 - ntile(5) over (order by lastVisit desc, customerId) as recencyScore,
                   6 - ntile(5) over (order by visitCount desc, customerId) as frequencyScore,
                   6 - ntile(5) over (order by totalAmount desc, customerId) as monetaryScore
            from customer_visits
            order by lastVisit desc, customerId
            """, nativeQuery = true)
    List<RfmTotal> summarizeRfm(@Param("storeId") long storeId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /**
     * 고객 등급 표에 필요한 집계값만 받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-10
     */
    interface RfmTotal {
        /** 고객 이름을 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        String getCustomerName();

        /** 기간 내 마지막 방문일을 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        String getLastVisitDate();

        /** 기간 내 방문 건수를 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        long getVisitCount();

        /** 기간 내 결제 합계를 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        long getTotalAmount();

        /** 최근성 점수를 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        int getRecencyScore();

        /** 방문 빈도 점수를 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        int getFrequencyScore();

        /** 결제 금액 점수를 반환한다. 작성자 한상희, 작성일 2026-11-10 */
        int getMonetaryScore();
    }

    /**
     * 고객별 집계 쿼리에서 금액과 건수만 받는 결과 인터페이스이다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     */
    interface CustomerTotals {
        /**
         * 고객의 조회 기간 결제 합계를 원 단위로 반환한다.
         *
         * 작성자 한상희
         * 작성일 2026-11-06
         */
        long getRevenue();

        /**
         * 고객의 조회 기간 방문 기록 수를 반환한다.
         *
         * 작성자 한상희
         * 작성일 2026-11-06
         */
        long getVisitCount();
    }

    /**
     * 요일별 집계 쿼리에서 요일 번호와 방문 건수를 받는 결과 인터페이스이다.
     *
     * 작성자 한상희
     * 작성일 2026-11-06
     */
    interface WeekdayTotal {
        /**
         * DB의 요일 번호를 반환한다. 일요일은 1, 토요일은 7이다.
         *
         * 작성자 한상희
         * 작성일 2026-11-06
         */
        int getDayNumber();

        /**
         * 조회 기간 중 해당 요일의 방문 건수를 반환한다.
         *
         * 작성자 한상희
         * 작성일 2026-11-06
         */
        long getVisitCount();
    }

    /**
     * 시간대별 쿼리의 시간과 건수를 받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     */
    interface HourlyTotal {
        /** 저장된 방문 시각에서 시간(0~23)을 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        int getVisitHour();

        /** 해당 시간대의 방문 건수를 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        long getVisitCount();
    }

    /**
     * 월별 쿼리의 연월, 매출, 고유 방문 고객 수를 받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-09
     */
    interface MonthlyTotal {
        /** 방문 연도를 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        int getVisitYear();

        /** 방문 월을 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        int getVisitMonth();

        /** 월 결제 합계를 원 단위로 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        long getRevenue();

        /** 한 번 이상 방문한 고유 고객 수를 반환한다. 작성자 한상희, 작성일 2026-11-09 */
        long getCustomerCount();
    }
}
