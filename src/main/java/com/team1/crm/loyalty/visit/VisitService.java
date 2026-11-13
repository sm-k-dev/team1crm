package com.team1.crm.loyalty.visit;

import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import com.team1.crm.loyalty.point.PointHistory;
import com.team1.crm.loyalty.point.PointHistoryRepository;
import com.team1.crm.loyalty.point.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 방문 등록과 적립을 처리한다.
 * 방문 저장과 적립 저장은 하나의 트랜잭션으로 묶여서, 둘 중 하나만 성공하는 일이 없다 (규칙 02의 6번).
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

    /**
     * 고객의 방문을 등록하고 결제 금액에 따라 포인트를 적립한다.
     *
     * @param storeId    로그인 회원이 소유를 확인한 매장 ID (컨트롤러에서 검증 후 전달)
     * @param customerId 방문한 고객 ID
     * @param form       검증을 마친 방문 입력값
     * @return 방문 ID와 이번에 적립된 포인트
     * @throws IllegalArgumentException 해당 매장의 고객이 아닌 경우 (CustomerService.findCustomer)
     */
    @Transactional
    public VisitResult register(Long storeId, Long customerId, VisitForm form) {
        // 1) 고객이 이 매장 소속인지 확인 (다른 매장 고객이면 여기서 예외)
        Customer customer = customerService.findCustomer(storeId, customerId);

        // 2) 방문 일시가 비어 있으면 서버 현재 시각 사용
        LocalDateTime visitedAt = form.getVisitedAt() != null ? form.getVisitedAt() : LocalDateTime.now();

        // 3) 방문 저장 (포인트 사용은 이 API에서 다루지 않으므로 usedPoint = 0)
        Visit visit = visitRepository.save(
                new Visit(customer, visitedAt, form.getAmount(), 0, form.getMenuMemo()));

        // 4) 매장 적립률로 적립액 계산 후 적립 내역 저장
        int earned = calculateEarnedPoints(form.getAmount(), customer.getStore().getPointRate());
        if (earned > 0) {
            pointHistoryRepository.save(
                    new PointHistory(customer, visit, PointType.EARN, earned, visitedAt));
        }

        return new VisitResult(visit.getId(), earned);
    }

    /**
     * 적립액 = 결제 금액 × 적립률 / 100, 원 단위 버림.
     * 정수 나눗셈을 쓰므로 소수점 오차가 생기지 않는다.
     * (반올림 규칙은 팀 회의에서 확정 전이며, 현재는 버림으로 구현)
     */
    static int calculateEarnedPoints(int amount, int pointRate) {
        return (int) ((long) amount * pointRate / 100);
    }
}
