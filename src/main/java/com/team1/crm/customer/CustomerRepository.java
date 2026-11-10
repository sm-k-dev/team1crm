package com.team1.crm.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 특정 매장에 등록된 고객 수 조회
    long countByStoreId(Long storeId);

}