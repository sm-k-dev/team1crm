package com.team1.crm.loyalty.coupon;

import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 발급, 목록 조회, 사용(redeem)을 처리한다.
 * 사용 처리는 CouponRepository.markUsed의 조건부 UPDATE로 중복 사용을 막는다.
 *
 * 작성자: 이재준
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CustomerService customerService;
    private final CouponRepository couponRepository;

    /** 고객에게 쿠폰을 발급하고 쿠폰 ID를 돌려준다. */
    @Transactional
    public Long issue(Long storeId, Long customerId, CouponForm form) {
        Customer customer = customerService.findCustomer(storeId, customerId);

        if (form.getExpiresAt().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("만료일은 오늘 이후여야 합니다.");
        }

        Coupon coupon = couponRepository.save(
                new Coupon(customer, form.getTitle(), form.getDiscount(), form.getExpiresAt()));
        return coupon.getId();
    }

    /** 고객의 쿠폰 목록 (최근 발급 순). */
    public List<Coupon> list(Long storeId, Long customerId) {
        Customer customer = customerService.findCustomer(storeId, customerId);
        return couponRepository.findByCustomerIdOrderByIdDesc(customer.getId());
    }

    /**
     * 쿠폰을 사용 처리하고 사용 일시를 돌려준다.
     * 소유 확인 → 조건부 UPDATE → 0행이면 이유를 찾아 예외로 알린다.
     */
    @Transactional
    public LocalDateTime redeem(Long storeId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        // 쿠폰이 이 매장 고객의 것인지 확인 (다른 매장 쿠폰이면 여기서 예외)
        Long customerId = coupon.getCustomer().getId();
        customerService.findCustomer(storeId, customerId);

        LocalDateTime now = LocalDateTime.now();
        int updated = couponRepository.markUsed(couponId, customerId, now, LocalDate.now());
        if (updated == 1) {
            return now;
        }

        // 갱신되지 않은 이유를 알려 준다 (이 시점의 엔티티 값으로 판단)
        if (coupon.getUsedAt() != null) {
            throw new IllegalArgumentException("이미 사용한 쿠폰입니다.");
        }
        throw new IllegalArgumentException("기한이 지난 쿠폰입니다.");
    }
}
