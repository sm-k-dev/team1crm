package com.team1.crm.loyalty;

import com.team1.crm.loyalty.coupon.Coupon;
import com.team1.crm.loyalty.point.PointHistory;
import com.team1.crm.loyalty.visit.Visit;

import java.util.List;

/**
 * 고객 상세 화면에 보여 줄 방문·포인트·쿠폰 정보 묶음.
 * 화면이 서비스 3개를 따로 부르지 않도록 LoyaltyDetailService가 한 번에 만들어 준다.
 *
 * 작성자: 이재준
 */
public record LoyaltyDetail(
        List<Visit> visits,
        long visitCount,
        List<Coupon> coupons,
        long pointBalance,
        List<PointHistory> pointHistory) {

    /** 값이 없을 때(테스트 등) 쓰는 빈 묶음. */
    public static LoyaltyDetail empty() {
        return new LoyaltyDetail(List.of(), 0L, List.of(), 0L, List.of());
    }
}
