package com.team1.crm.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    // 특정 매장에 등록된 고객 수 조회
    long countByStoreId(Long storeId);

    /**
     * 지정한 매장에 같은 전화번호를 가진 고객이 있는지 확인한다.
     * 전화번호는 하이픈을 포함한 형식으로 통일한 값을 전달해야 한다.
     *
     * @param storeId 중복 여부를 확인할 매장의 ID
     * @param phone 형식이 통일된 전화번호
     * @return 같은 매장에 같은 전화번호가 있으면 true, 없으면 false
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    boolean existsByStore_IdAndPhone(Long storeId, String phone);

    /**
     * 고객 ID와 소속 매장 ID가 모두 일치하는 고객을 조회한다.
     *
     * @param customerId 조회할 고객의 ID
     * @param storeId 고객이 속해야 하는 매장의 ID
     * @return 조건에 맞는 고객. 일치하는 고객이 없으면 빈 Optional
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    Optional<Customer> findByIdAndStore_Id(Long customerId, Long storeId);

    /**
     * 수정할 고객을 제외하고 같은 매장에 같은 전화번호가 있는지 확인한다.
     *
     * @param storeId 중복 여부를 확인할 매장의 ID
     * @param phone 형식이 통일된 전화번호
     * @param customerId 중복 검사에서 제외할 고객의 ID
     * @return 다른 고객이 같은 번호를 사용하고 있으면 true
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    boolean existsByStore_IdAndPhoneAndIdNot(
            Long storeId,
            String phone,
            Long customerId
    );

    /**
     * 지정한 매장에서 이름에 검색어가 포함되거나 전화번호 뒤 4자리가 일치하는 고객을 조회한다.
     * 결과는 고객 ID 오름차순으로 정렬한다.
     *
     * @param storeId 검색할 매장의 ID
     * @param keyword 앞뒤 공백을 제거한 검색어
     * @return 조건에 맞는 해당 매장의 고객 목록
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Query("""
        select c
        from Customer c
        where c.store.id = :storeId
          and (
              locate(:keyword, c.name) > 0
              or substring(c.phone, length(c.phone) - 3, 4) = :keyword
          )
        order by c.id asc
        """)
    List<Customer> searchCustomers(
            @Param("storeId") Long storeId,
            @Param("keyword") String keyword
    );
}