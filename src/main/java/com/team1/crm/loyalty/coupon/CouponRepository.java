package com.team1.crm.loyalty.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Coupon 은 같은 패키지라 import 불필요
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // 고객의 쿠폰 목록 (최근 발급 순)
    List<Coupon> findByCustomerIdOrderByIdDesc(Long customerId);

    // 쿠폰 사용 처리를 조건부 UPDATE 한 번으로 끝낸다 (중복 사용 방지).
    // 미사용 + 기한 내 + 같은 고객인 경우에만 usedAt 을 채우고, 바뀐 행 수(0 또는 1)를 돌려준다.
    // 반환값이 0 이면 서비스에서 "이미 사용했거나 기한이 지났거나 대상이 없음"으로 처리한다.
    @Modifying
    @Query("UPDATE Coupon c SET c.usedAt = :now " +
           "WHERE c.id = :couponId AND c.customer.id = :customerId " +
           "AND c.usedAt IS NULL AND c.expiresAt >= :today")
    int markUsed(@Param("couponId") Long couponId,
                 @Param("customerId") Long customerId,
                 @Param("now") LocalDateTime now,
                 @Param("today") LocalDate today);
}
