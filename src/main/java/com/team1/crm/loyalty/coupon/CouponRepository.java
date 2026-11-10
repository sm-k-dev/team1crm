package com.team1.crm.loyalty.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Coupon 은 같은 패키지라 import 불필요
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // 고객의 쿠폰 목록 (최근 발급 순)
    List<Coupon> findByCustomerIdOrderByIdDesc(Long customerId);
}
