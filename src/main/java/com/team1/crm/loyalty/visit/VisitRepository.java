package com.team1.crm.loyalty.visit;

// Visit 은 같은 패키지라 import 불필요
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // 고객의 최근 방문 20건 (최신순)
    List<Visit> findTop20ByCustomerIdOrderByVisitedAtDesc(Long customerId);

    // 고객의 방문 횟수
    long countByCustomerId(Long customerId);
}