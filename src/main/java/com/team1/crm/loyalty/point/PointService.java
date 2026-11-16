package com.team1.crm.loyalty.point;

import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 포인트 잔액 조회와 내역 조회, 방문 등록 중 포인트 사용을 돕는 잠금·잔액 확인을 담당한다.
 * 잔액은 저장하지 않고 적립 합계 - 사용 합계로 계산한다 (PointHistoryRepository.balance).
 * 포인트 사용 내역 저장은 방문 등록(VisitService)이 한 트랜잭션 안에서 처리한다.
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
     * 포인트를 쓰는 방문 등록에서, 같은 고객의 다른 사용 요청이 끝날 때까지 기다리도록 고객 행을 잠근다.
     * 반드시 잔액을 읽기 전에 호출해야 한다. MySQL 트랜잭션은 처음 읽을 때 스냅샷을 만들기 때문에,
     * 잠금보다 먼저 읽으면 잠금 후 커밋된 사용 내역이 잔액에 보이지 않아 잔액 초과 사용이 가능해진다.
     * 호출한 트랜잭션(방문 등록)에 합류하며, 트랜잭션이 끝나면 잠금이 풀린다.
     */
    @Transactional
    public void lockCustomer(Long customerId) {
        Customer locked = entityManager.find(Customer.class, customerId, LockModeType.PESSIMISTIC_WRITE);
        if (locked == null) {
            throw new IllegalArgumentException("해당 매장의 고객을 찾을 수 없습니다.");
        }
    }

    /** 고객의 현재 잔액. 매장 소속 확인은 호출한 쪽이 이미 마친 상태에서 쓴다. */
    public long balanceOf(Long customerId) {
        return pointHistoryRepository.balance(customerId);
    }
}
