package com.team1.crm.loyalty.visit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 매장 잠금을 얻은 뒤 locking read로 직전 커밋의 결과를 확인한다. 김순민, 2026-11-27. */
public interface VisitRequestRepository extends JpaRepository<VisitRequest,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VisitRequest r where r.customer.id=:customerId and r.requestKey=:key")
    Optional<VisitRequest> findCompleted(@Param("customerId") Long customerId,@Param("key") String key);
}
