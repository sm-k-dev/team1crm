package com.team1.crm.loyalty;

import com.team1.crm.loyalty.coupon.CouponService;
import com.team1.crm.loyalty.point.PointService;
import com.team1.crm.loyalty.visit.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객 상세 화면에 필요한 방문·포인트·쿠폰 정보를 한 번에 모아 준다.
 * 다른 도메인(customer 화면)은 이 서비스 하나만 호출하면 되고,
 * 각 하위 서비스가 고객의 매장 소속을 확인하므로 다른 매장 고객 정보는 나오지 않는다.
 *
 * 작성자: 이재준
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyDetailService {

    private final VisitService visitService;
    private final PointService pointService;
    private final CouponService couponService;

    public LoyaltyDetail getDetail(Long storeId, Long customerId) {
        return new LoyaltyDetail(
                visitService.getRecentVisits(storeId, customerId),
                visitService.getVisitCount(storeId, customerId),
                couponService.list(storeId, customerId),
                pointService.getBalance(storeId, customerId),
                pointService.getRecentHistory(storeId, customerId));
    }
}
