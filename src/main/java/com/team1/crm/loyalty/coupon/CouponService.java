package com.team1.crm.loyalty.coupon;

import com.team1.crm.auth.StoreService;
import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 발급, 목록 조회, 방문 등록 중 쿠폰 사용을 처리한다.
 * 쿠폰 사용은 방문 등록(VisitService)에서만 일어나며, CouponRepository.markUsed의 조건부 UPDATE로 중복 사용을 막는다.
 *
 * 작성자: 이재준
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CustomerService customerService;
    private final CouponRepository couponRepository;
    private final StoreService storeService;

    /** 고객에게 쿠폰을 발급하고 쿠폰 ID를 돌려준다. */
    @Transactional
    public Long issue(Long storeId, Long customerId, CouponForm form) {
        storeService.lockOpenStore(storeId); // 폐업 매장 차단 + 폐업 처리와 동시 실행 방지 (매장 → 고객 잠금 순서)
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
     * 방문 등록과 함께 쿠폰을 사용 처리하고 할인액을 돌려준다.
     * 호출한 방문 등록 트랜잭션에 합류하므로, 이후 단계가 실패하면 쿠폰 사용도 함께 취소된다.
     *
     * @throws IllegalArgumentException 쿠폰이 없거나 이 고객의 쿠폰이 아니거나, 이미 사용했거나 기한이 지난 경우
     */
    @Transactional
    public int useForVisit(Long customerId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        if (!coupon.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("이 고객의 쿠폰이 아닙니다.");
        }

        // 조건부 UPDATE: "미사용 + 기한 내 + 같은 고객"일 때만 사용 처리한다.
        // 확인과 변경이 DB에서 한 문장이라 동시에 두 요청이 와도 한쪽만 1행을 바꾼다.
        int updated = couponRepository.markUsed(coupon.getId(), customerId, LocalDateTime.now(), LocalDate.now());
        if (updated == 1) {
            return coupon.getDiscount();
        }

        // 갱신되지 않은 이유를 알려 준다.
        // usedAt은 동시 요청이 방금 커밋한 값을 못 볼 수 있어서(읽기 시점의 스냅샷), 바뀌지 않는 만료일로 먼저 판단한다.
        // 만료되지 않았는데 갱신이 0행이면 이미 다른 요청이 사용 처리한 쿠폰이다.
        if (coupon.getExpiresAt().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("기한이 지난 쿠폰입니다.");
        }
        throw new IllegalArgumentException("이미 사용한 쿠폰입니다.");
    }
}
