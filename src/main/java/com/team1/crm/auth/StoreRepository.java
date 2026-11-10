
package com.team1.crm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    // 로그인한 회원이 소유한 모든 매장 조회
    List<Store> findByOwnerId(Long memberId);

}
