package com.team1.crm.loyalty;

import com.team1.crm.loyalty.coupon.CouponRepository;
import com.team1.crm.loyalty.visit.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인정보를 제거한(anonymized) 고객의 자유 입력 글을 정리한다.
 *
 * 고객 테이블의 이름·전화번호를 지워도 방문 메모나 쿠폰 제목에 "김단골님 010-1234-5678"처럼 적혀 있으면
 * 개인정보가 남는다. 그래서 같은 트랜잭션에서 이 글들도 함께 비운다.
 * 결제 금액·방문일·사용 포인트·할인액·사용 이력은 통계에 필요하므로 건드리지 않는다.
 *
 * 대상 조건은 "고객이 이미 개인정보를 제거했는가(anonymized_at 이 있는가)" 하나뿐이다.
 * 고객 파기의 보관 기간 조건을 다시 쓰지 않으므로 두 조건이 어긋날 수 없고,
 * 이 기능이 생기기 전에 이미 파기된 고객의 남은 글도 다음 실행에서 함께 정리된다.
 * 이미 정리된 행은 조건에서 제외되어 몇 번 실행해도 결과가 같다.
 *
 * 작성자: 이재준
 * 작성일: 2026-11-17
 */
@Service
@RequiredArgsConstructor
public class LoyaltyPrivacyService {

    /** 정리한 쿠폰 제목. 폐업 매장은 "폐업 매장 쿠폰"으로 바꾼다(10_매장_폐업_정책.md). */
    static final String REMOVED_COUPON_TITLE = "삭제된 고객 쿠폰";

    private final VisitRepository visitRepository;
    private final CouponRepository couponRepository;

    /**
     * 개인정보를 제거한 고객의 방문 메모를 비우고 쿠폰 제목을 고정 문구로 바꾼다.
     * 호출한 쪽(CustomerPrivacyService)의 트랜잭션에 합류하므로, 도중에 실패하면 고객 파기까지 함께 취소되어
     * 다음 예약 실행에서 다시 시도된다.
     *
     * @return 정리한 방문 메모 수와 쿠폰 제목 수의 합
     */
    @Transactional
    public int clearFreeTextOfAnonymizedCustomers() {
        int memos = visitRepository.clearMemoOfAnonymizedCustomers();
        int titles = couponRepository.replaceTitleOfAnonymizedCustomers(REMOVED_COUPON_TITLE);
        return memos + titles;
    }
}
