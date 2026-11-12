package com.team1.crm.auth;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

/** 폐업 매장의 정보만 제거하고 거래 수치·관계는 보존한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public interface StoreClosureRepository extends Repository<Store, Long> {
    /** 고객 주요 정보를 즉시 제거한다. 기존 삭제 시각은 유지한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Customer c set c.name = '삭제된 고객', c.phone = null, c.birthMonth = null,
                c.memo = null, c.agreeMarketing = false, c.deletedAt = coalesce(c.deletedAt, :at),
                c.anonymizedAt = coalesce(c.anonymizedAt, :at) where c.store.id = :id
            """)
    int removeCustomerData(@Param("id") Long id, @Param("at") LocalDateTime at);

    /** 방문 메모에 복사된 개인정보도 제거한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Modifying
    @Query("update Visit v set v.menuMemo = null where v.customer.id in (select c.id from Customer c where c.store.id = :id)")
    int clearVisitMemos(@Param("id") Long id);

    /** 쿠폰 제목 자유 입력은 대체하고 금액·사용 이력은 유지한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Modifying
    @Query("update Coupon cp set cp.title = '폐업 매장 쿠폰' where cp.customer.id in (select c.id from Customer c where c.store.id = :id)")
    int clearCouponTitles(@Param("id") Long id);
}
