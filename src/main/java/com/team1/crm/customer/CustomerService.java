package com.team1.crm.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객 조회와 고객 관리 업무를 처리하는 서비스이다.
 * 현재는 매장별 고객 목록 조회 기능을 제공한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * 지정한 매장에 속한 고객 목록을 고객 ID 오름차순으로 조회한다.
     * 현재 매장 소유권 검증은 구현되어 있지 않으며,
     * 인증 연동 시 조회 전에 로그인한 사용자의 매장 접근 권한을 확인해야 한다.
     *
     * @param storeId 조회할 매장의 ID
     * @return 해당 매장의 고객 목록. 고객이 없으면 빈 목록
     * @throws IllegalArgumentException 매장 ID가 없거나 양수가 아닌 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-06
     */
    public List<Customer> findCustomers(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 ID는 양수여야 합니다.");
        }

        return customerRepository.findAllByStore_IdOrderByIdAsc(storeId);
    }
}