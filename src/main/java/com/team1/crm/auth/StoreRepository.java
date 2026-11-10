
package com.team1.crm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    // 로그인한 회원이 소유한 모든 매장 조회
    List<Store> findByOwnerId(Long memberId);

    /**
     * 매장 ID와 소유 회원 ID가 모두 일치하는 매장을 조회한다.
     *
     * @param storeId 조회할 매장의 ID
     * @param memberId 로그인한 회원의 ID
     * @return 조건에 맞는 매장. 일치하는 매장이 없으면 빈 Optional
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    Optional<Store> findByIdAndOwner_Id(Long storeId, Long memberId);

}
