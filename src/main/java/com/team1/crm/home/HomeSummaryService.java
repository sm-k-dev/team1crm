package com.team1.crm.home;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/** 소유권이 확인된 매장의 홈 요약을 조회한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeSummaryService {
    private final HomeSummaryRepository repository;

    /** 한국 날짜 기준 오늘과 이번 달을 시작 포함·종료 제외로 집계한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public HomeSummary summarize(Long storeId, LocalDate today) {
        var monthStart = today.withDayOfMonth(1).atStartOfDay();
        return new HomeSummary(repository.countCustomers(storeId),
                repository.countVisits(storeId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                repository.sumRevenue(storeId, monthStart, monthStart.plusMonths(1)));
    }
}
