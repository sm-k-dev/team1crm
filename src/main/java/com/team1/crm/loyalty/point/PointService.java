package com.team1.crm.loyalty.point;

import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 잔액 조회, 내역 조회, 사용을 처리한다.
 * 잔액은 저장하지 않고 적립 합계 - 사용 합계로 계산한다 (PointHistoryRepository.balance).
 *
 * 작성자: 이재준
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final CustomerService customerService;
    private final PointHistoryRepository pointHistoryRepository;
    private final EntityManager entityManager;

    public long getBalance(Long storeId, Long customerId) {
        Customer customer = customerService.findCustomer(storeId, customerId);
        return pointHistoryRepository.balance(customer.getId());
    }

    public List<PointHistory> getRecentHistory(Long storeId, Long customerId) {
        Customer customer = customerService.findCustomer(storeId, customerId);
        return pointHistoryRepository.findTop20ByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId());
    }

    /**
     * 포인트를 사용한다. 잔액보다 많이 쓰는 것을 막기 위해 고객 행을 잠근 뒤 잔액을 확인한다.
     *
     * 동시에 두 요청이 들어와도 한쪽이 잠금을 기다렸다가 갱신된 잔액을 보고 거절된다.
     * 주의: 잠금은 소유 확인보다 먼저 걸어야 한다. MySQL 트랜잭션은 처음 읽을 때 스냅샷을 만들기 때문에,
     * 잠금보다 먼저 읽으면 잠금 후 커밋된 사용 내역이 잔액 계산에 보이지 않을 수 있다.
     */
    @Transactional
    public PointUseResult use(Long storeId, Long customerId, int points) {
        // 1) 고객 행 잠금 (다른 사용 요청은 이 트랜잭션이 끝날 때까지 기다린다)
        Customer locked = entityManager.find(Customer.class, customerId, LockModeType.PESSIMISTIC_WRITE);
        if (locked == null) {
            throw new IllegalArgumentException("해당 매장의 고객을 찾을 수 없습니다.");
        }

        // 2) 매장 소유 확인 (잠금 이후에 읽어야 최신 잔액이 보인다)
        Customer customer = customerService.findCustomer(storeId, customerId);

        // 3) 잔액 확인
        long balance = pointHistoryRepository.balance(customer.getId());
        if (points <= 0) {
            throw new IllegalArgumentException("사용 포인트는 1 이상이어야 합니다.");
        }
        if (points > balance) {
            throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다. 현재 잔액: " + balance + "P");
        }

        // 4) 사용 내역 저장 (방문과 연결하지 않으므로 visit = null)
        PointHistory history = pointHistoryRepository.save(
                new PointHistory(customer, null, PointType.USE, points, LocalDateTime.now()));

        return new PointUseResult(balance - points, history.getId());
    }
}
