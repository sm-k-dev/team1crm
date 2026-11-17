package com.team1.crm.loyalty.visit;

import com.team1.crm.auth.StoreService;
import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import com.team1.crm.loyalty.coupon.CouponService;
import com.team1.crm.loyalty.point.PointHistory;
import com.team1.crm.loyalty.point.PointHistoryRepository;
import com.team1.crm.loyalty.point.PointService;
import com.team1.crm.loyalty.point.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 방문 등록을 처리한다. 쿠폰 사용, 포인트 사용, 포인트 적립이 모두 이 한 번의 등록 안에서 일어난다.
 * 쿠폰 사용·포인트 사용·방문 저장·적립 저장은 하나의 트랜잭션으로 묶여서, 일부만 성공하는 일이 없다 (규칙 02의 6번).
 *
 * 작성자: 이재준
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private final CustomerService customerService;
    private final VisitRepository visitRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CouponService couponService;
    private final PointService pointService;
    private final StoreService storeService;

    /**
     * 고객의 방문을 등록한다. 결제 금액 계산 순서는 다음과 같다.
     *   입력 금액(할인 전) → 쿠폰 할인 → 포인트 사용(1P = 1원) → 실제 결제 금액 → 이 금액 기준으로 적립
     *
     * @param storeId    로그인 회원이 소유를 확인한 매장 ID (컨트롤러에서 검증 후 전달)
     * @param customerId 방문한 고객 ID
     * @param form       검증을 마친 방문 입력값 (결제 금액은 할인 전 금액)
     * @return 방문 ID, 적립 포인트, 쿠폰 할인액, 사용 포인트, 실제 결제 금액
     * @throws IllegalArgumentException 해당 매장의 고객이 아니거나, 쿠폰을 쓸 수 없거나, 포인트가 부족한 경우
     * @throws org.springframework.web.server.ResponseStatusException 폐업 매장이면 409
     */
    @Transactional
    public VisitResult register(Long storeId, Long customerId, VisitForm form) {
        int usedPoints = form.getUsedPoints() != null ? form.getUsedPoints() : 0;
        if (usedPoints < 0) {
            // 폼 검증(@Min(0))이 먼저 막지만, 음수가 들어오면 결제 금액이 늘어나므로 서비스도 직접 막는다
            throw new IllegalArgumentException("사용 포인트는 0 이상이어야 합니다.");
        }

        // 0-1) 매장을 잠그고 영업 중인지 확인한다 (폐업 처리와 신규 거래가 동시에 실행되지 않도록).
        //      잠금 순서는 폐업 처리와 같은 "매장 → 고객"이어야 서로 기다리는 교착 상태가 생기지 않는다
        storeService.lockOpenStore(storeId);

        // 0-2) 포인트를 쓰는 경우 고객 행을 먼저 잠근다. 잔액은 잠근 뒤에 읽어야 최신 값이 보인다 (PointService 참고)
        if (usedPoints > 0) {
            pointService.lockCustomer(customerId);
        }

        // 1) 고객이 이 매장 소속인지 확인 (다른 매장·삭제된 고객이면 여기서 예외)
        Customer customer = customerService.findCustomer(storeId, customerId);

        // 2) 방문 일시가 비어 있으면 서버 현재 시각 사용
        LocalDateTime visitedAt = form.getVisitedAt() != null ? form.getVisitedAt() : LocalDateTime.now();

        // 3) 쿠폰을 골랐다면 사용 처리하고 할인액을 구한다. 쿠폰이 결제 금액보다 크면 0원까지만 내려간다
        int discount = form.getCouponId() != null
                ? couponService.useForVisit(customer.getId(), form.getCouponId())
                : 0;
        int afterCoupon = Math.max(form.getAmount() - discount, 0);
        int appliedDiscount = form.getAmount() - afterCoupon;

        // 4) 포인트 사용 검증: 잔액 이내, 그리고 쿠폰 할인 후 결제 금액 이내
        if (usedPoints > 0) {
            long balance = pointService.balanceOf(customer.getId());
            if (usedPoints > balance) {
                throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다. 현재 잔액: " + balance + "P");
            }
            if (usedPoints > afterCoupon) {
                throw new IllegalArgumentException("사용 포인트는 쿠폰 할인 후 결제 금액(" + afterCoupon + "원)을 넘을 수 없습니다.");
            }
        }

        // 5) 실제 결제 금액 = 쿠폰과 포인트를 모두 뺀 금액 (홈 매출 집계와 같은 기준)
        int paidAmount = afterCoupon - usedPoints;

        // 6) 방문 저장
        Visit visit = visitRepository.save(
                new Visit(customer, visitedAt, paidAmount, usedPoints, form.getMenuMemo()));

        // 7) 포인트 사용 내역 저장 (이번 방문과 연결)
        if (usedPoints > 0) {
            pointHistoryRepository.save(
                    new PointHistory(customer, visit, PointType.USE, usedPoints, visitedAt));
        }

        // 8) 실제 결제 금액 기준으로 매장 적립률만큼 적립 내역 저장
        int earned = calculateEarnedPoints(paidAmount, customer.getStore().getPointRate());
        if (earned > 0) {
            pointHistoryRepository.save(
                    new PointHistory(customer, visit, PointType.EARN, earned, visitedAt));
        }

        return new VisitResult(visit.getId(), earned, appliedDiscount, usedPoints, paidAmount);
    }

    /**
     * 고객 상세 화면의 "최근 방문" 목록. 최근 20건, 최신순.
     * 고객이 이 매장 소속인지 먼저 확인한다 (다른 매장 고객이면 예외).
     */
    public List<Visit> getRecentVisits(Long storeId, Long customerId) {
        Customer customer = customerService.findCustomer(storeId, customerId);
        return visitRepository.findTop20ByCustomerIdOrderByVisitedAtDesc(customer.getId());
    }

    /** 고객의 전체 방문 횟수 (상세 화면 상단 숫자). */
    public long getVisitCount(Long storeId, Long customerId) {
        Customer customer = customerService.findCustomer(storeId, customerId);
        return visitRepository.countByCustomerId(customer.getId());
    }

    /**
     * 적립액 = 결제 금액 × 적립률 / 100, 소수점 첫째 자리에서 반올림 (예: 29.5P → 30P, 29.49P → 29P).
     * 100으로 나누기 전에 50을 더하면 정수 계산만으로 반올림이 되어 부동소수점 오차가 생기지 않는다.
     * 금액 × 적립률이 int 범위를 넘을 수 있어 long으로 계산한다.
     * (2026-11-16 반올림으로 확정, 이전에는 버림)
     */
    static int calculateEarnedPoints(int amount, int pointRate) {
        return (int) (((long) amount * pointRate + 50) / 100);
    }
}
