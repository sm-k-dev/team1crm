package com.team1.crm.admin;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

/** 인증 정보를 제외하고 플랫폼 운영 집계만 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public interface AdminDashboardRepository extends Repository<Member, Long> {
    /** 사장님 역할 계정 수이다. 작성자: 김순민 / 작성일: 2026-11-11 */
    long countByRole(Role role);

    /** 전체 매장 수이다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("select count(s) from Store s")
    long countStores();

    /** 여러 매장을 소유해도 사장님은 한 번만 집계한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("select count(m) from Member m where m.role = :role and exists (select s.id from Store s where s.owner = m)")
    long countLinkedOwners(@Param("role") Role role);

    /** 매장이 없는 사장님 정보를 ID 순으로 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("""
            select new com.team1.crm.admin.AdminUnlinkedOwner(m.id, m.nickname, m.email, m.createdAt)
            from Member m where m.role = :role
              and not exists (select s.id from Store s where s.owner = m)
            order by m.id
            """)
    List<AdminUnlinkedOwner> findUnlinkedOwners(@Param("role") Role role);

    /** 시작 포함·종료 제외 기간의 월별 사장님 등록 수이다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("""
            select new com.team1.crm.admin.AdminMonthCount(year(m.createdAt), month(m.createdAt), count(m))
            from Member m where m.role = :role and m.createdAt >= :start and m.createdAt < :end
            group by year(m.createdAt), month(m.createdAt)
            """)
    List<AdminMonthCount> countOwnersByMonth(@Param("role") Role role, @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /** 시작 포함·종료 제외 기간의 월별 매장 등록 수이다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Query("""
            select new com.team1.crm.admin.AdminMonthCount(year(s.createdAt), month(s.createdAt), count(s))
            from Store s where s.createdAt >= :start and s.createdAt < :end
            group by year(s.createdAt), month(s.createdAt)
            """)
    List<AdminMonthCount> countStoresByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
