package com.team1.crm.loyalty.coupon;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 쿠폰 발급 API. 명세: POST /api/customers/{id}/coupons (발급 → couponId)
 * 쿠폰 사용은 별도 API 없이 방문 등록(couponId)에서만 한다.
 *
 * 작성자: 이재준
 */
@RestController
@RequiredArgsConstructor
public class CouponApiController {

    private final CouponService couponService;
    private final StoreService storeService;

    @PostMapping("/api/customers/{customerId}/coupons")
    public ResponseEntity<Map<String, Long>> issue(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId,
            @Valid @RequestBody CouponForm form) {

        Store store = storeService.findOwnedStore(member.getId(), storeId);
        Long couponId = couponService.issue(store.getId(), customerId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("couponId", couponId));
    }
}
