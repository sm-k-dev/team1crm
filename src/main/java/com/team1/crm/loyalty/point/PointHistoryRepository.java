package com.team1.crm.loyalty.point;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// PointHistory, PointType 은 같은 패키지라 import 불필요
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 고객의 최근 포인트 내역 20건 (최신순, 같은 시각이면 id 큰 순)
    List<PointHistory> findTop20ByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    // 잔액 = 적립 합계 - 사용 합계. CASE 로 사용은 음수로 바꿔 한 번에 더한다. 기록이 없으면 COALESCE 로 0
    @Query("SELECT COALESCE(SUM(CASE WHEN p.type = com.team1.crm.loyalty.point.PointType.EARN THEN p.point ELSE -p.point END), 0) " +
           "FROM PointHistory p WHERE p.customer.id = :customerId")
    long balance(@Param("customerId") Long customerId);
}
