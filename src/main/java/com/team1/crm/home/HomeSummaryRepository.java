package com.team1.crm.home;

import com.team1.crm.customer.Customer;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

/** 홈 화면에 필요한 집계만 매장 단위로 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public interface HomeSummaryRepository extends Repository<Customer, Long> {
    /** 현재 고객 수에서는 소프트 삭제 고객을 제외한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("select count(c) from Customer c where c.store.id = :storeId and c.deletedAt is null")
    long countCustomers(@Param("storeId") Long storeId);

    /** 과거 이력을 보존하여 해당 기간의 방문 건수를 센다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("select count(v) from Visit v where v.customer.store.id = :storeId and v.visitedAt >= :start and v.visitedAt < :end")
    long countVisits(@Param("storeId") Long storeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 삭제 고객의 결제도 포함하여 매출을 합산한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("select coalesce(sum(v.amount), 0) from Visit v where v.customer.store.id = :storeId and v.visitedAt >= :start and v.visitedAt < :end")
    long sumRevenue(@Param("storeId") Long storeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
