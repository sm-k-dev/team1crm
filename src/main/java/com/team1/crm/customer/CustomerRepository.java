package com.team1.crm.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 고객 정보를 DB에 저장하고 조회하는 Repository 인터페이스이다.
 * JpaRepository를 상속하므로 @Repository를 생략할 수 있다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * 지정한 매장에 속한 고객 목록을 고객 ID 오름차순으로 조회한다.
     * 매장 접근 권한은 호출하는 서비스에서 별도로 확인해야 한다.
     *
     * @param storeId 조회할 매장의 ID
     * @return 해당 매장의 고객 목록. 고객이 없으면 빈 목록
     *
     * 작성자: 김순민
     * 작성일: 2026-11-06
     */
    List<Customer> findAllByStore_IdOrderByIdAsc(Long storeId);
}