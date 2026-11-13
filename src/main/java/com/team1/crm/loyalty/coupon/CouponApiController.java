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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 쿠폰 발급과 사용 API.
 * 명세: POST /api/customers/{id}/coupons (발급 → couponId),
 *       POST /api/coupons/{id}/redeem (사용 → usedAt)
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

    @PostMapping("/api/coupons/{couponId}/redeem")
    public Map<String, LocalDateTime> redeem(
            @LoginMember Member member,
            @PathVariable("couponId") Long couponId,
            @RequestParam("storeId") Long storeId) {

        Store store = storeService.findOwnedStore(member.getId(), storeId);
        LocalDateTime usedAt = couponService.redeem(store.getId(), couponId);
        return Map.of("usedAt", usedAt);
    }
}
