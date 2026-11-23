package com.team1.crm.loyalty.visit;

// Visit 은 같은 패키지라 import 불필요
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // 고객의 최근 방문 20건 (최신순)
    List<Visit> findTop20ByCustomerIdOrderByVisitedAtDesc(Long customerId);

    // 고객의 방문 횟수
    long countByCustomerId(Long customerId);

    // 개인정보를 제거한(anonymized_at 이 있는) 고객의 방문 메모를 비운다. 이미 비어 있는 행은 제외해 반복 실행해도 결과가 같다.
    // 결제 금액·방문일·사용 포인트는 건드리지 않는다. 바뀐 행 수를 돌려준다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Visit v SET v.menuMemo = null WHERE v.menuMemo IS NOT NULL " +
           "AND v.customer.id IN (SELECT c.id FROM Customer c WHERE c.anonymizedAt IS NOT NULL)")
    int clearMemoOfAnonymizedCustomers();
}