package com.team1.crm.admin;

import com.team1.crm.auth.Store;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * 관리자용 전체 매장 및 연결된 사장님 정보를 조회한다.
 * 작성자: 김순민 / 작성일: 2026-11-11
 */
public interface AdminStoreRepository extends Repository<Store, Long> {
    /**
     * 소유자 정보와 매장을 한 번에 조회하여 매장 ID 순으로 반환한다.
     * 작성자: 김순민 / 작성일: 2026-11-11
     */
    @Query("""
            select new com.team1.crm.admin.AdminStoreRow(
                s.id, s.name, s.category, s.address, o.id, o.nickname, o.email, s.pointRate)
            from Store s left join s.owner o
            order by s.id
            """)
    List<AdminStoreRow> findStoreRows();
}
