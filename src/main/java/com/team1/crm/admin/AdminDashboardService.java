package com.team1.crm.admin;

import com.team1.crm.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 최근 6개월 등록과 사장님 연결 현황을 계산한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {
    private final AdminDashboardRepository repository;

    /** 기준 월을 포함한 6개월의 빈 월·연도 경계·0명 분모를 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public AdminDashboard summarize(YearMonth month) {
        var start = month.minusMonths(5).atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();
        long owners = repository.countByRole(Role.USER);
        long linked = repository.countLinkedOwners(Role.USER);
        var ownerMonths = monthMap(repository.countOwnersByMonth(Role.USER, start, end));
        var storeMonths = monthMap(repository.countStoresByMonth(start, end));
        var months = IntStream.rangeClosed(0, 5).mapToObj(i -> month.minusMonths(5 - i))
                .map(m -> new AdminDashboard.Month(m.toString(), ownerMonths.getOrDefault(m, 0L),
                        storeMonths.getOrDefault(m, 0L))).toList();
        long newOwners = ownerMonths.getOrDefault(month, 0L);
        long newStores = storeMonths.getOrDefault(month, 0L);
        return new AdminDashboard(owners, repository.countStores(), linked, owners - linked,
                owners == 0 ? 0 : Math.round(linked * 1000.0 / owners) / 10.0,
                newOwners, newStores, newOwners - ownerMonths.getOrDefault(month.minusMonths(1), 0L),
                newStores - storeMonths.getOrDefault(month.minusMonths(1), 0L), months,
                repository.findUnlinkedOwners(Role.USER));
    }

    /** 연월을 키로 만들어 빈 월을 계산할 수 있게 한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Map<YearMonth, Long> monthMap(List<AdminMonthCount> rows) {
        return rows.stream().collect(Collectors.toMap(row -> YearMonth.of(row.year(), row.month()), AdminMonthCount::count));
    }
}
