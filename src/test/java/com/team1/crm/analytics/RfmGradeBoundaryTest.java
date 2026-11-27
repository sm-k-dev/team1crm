package com.team1.crm.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** T28: 공개 집계 경로에서 125개 점수 조합과 시간 우선순위를 확인. 김순민, 2026-11-27. */
class RfmGradeBoundaryTest {
    static final YearMonth MONTH = YearMonth.of(2026, 11);
    static Stream<Arguments> scores() {
        return IntStream.rangeClosed(1,5).boxed().flatMap(r -> IntStream.rangeClosed(1,5).boxed()
                .flatMap(f -> IntStream.rangeClosed(1,5).mapToObj(m -> Arguments.of(r,f,m))));
    }
    AnalyticsService.RfmReport report(int r,int f,int m,String date) {
        var repository = mock(AnalyticsRepository.class);
        var total = mock(AnalyticsRepository.RfmTotal.class);
        when(total.getCustomerName()).thenReturn("등급검증"); when(total.getLastVisitDate()).thenReturn(date);
        when(total.getRecencyScore()).thenReturn(r); when(total.getFrequencyScore()).thenReturn(f);
        when(total.getMonetaryScore()).thenReturn(m);
        when(repository.summarizeRfm(eq(7L),any(),any())).thenReturn(List.of(total));
        var report = new AnalyticsService(repository).summarizeRfm(7L,MONTH);
        verify(repository).summarizeRfm(7L,MONTH.minusMonths(5).atDay(1).atStartOfDay(),MONTH.plusMonths(1).atDay(1).atStartOfDay());
        return report;
    }
    @ParameterizedTest(name="R={0}, F={1}, M={2}") @MethodSource("scores")
    void allScoreCombinationsFollowPublishedThresholds(int r,int f,int m) {
        String expected;
        if(r<=2 && f<=3) expected="이탈 위험";
        else if(r+f+m>=13) expected="VIP";
        else if(r+f+m>=10) expected="단골";
        else expected="일반";
        var actual=report(r,f,m,"2026-11-29");
        assertEquals(expected,actual.rows().getFirst().grade());
        assertEquals(List.of("VIP","단골","일반","이탈 위험"),actual.gradeCounts().stream().map(AnalyticsService.GradeCount::label).toList());
        assertEquals(1,actual.gradeCounts().stream().mapToInt(AnalyticsService.GradeCount::count).sum());
        assertEquals(1,actual.gradeCounts().stream().filter(g->g.label().equals(expected)).findFirst().orElseThrow().count());
    }
    @Test void totalTwelveAndThirteenSeparateRegularAndVip() {
        assertEquals("단골",report(4,4,4,"2026-11-29").rows().getFirst().grade());
        assertEquals("VIP",report(5,4,4,"2026-11-29").rows().getFirst().grade());
    }
    @Test void totalNineAndTenSeparateGeneralAndRegular() {
        assertEquals("일반",report(3,3,3,"2026-11-29").rows().getFirst().grade());
        assertEquals("단골",report(4,3,3,"2026-11-29").rows().getFirst().grade());
    }
    @Test void sixtyDaysInactiveOverridesMaximumScore() {
        assertEquals("이탈 위험",report(5,5,5,"2026-10-01").rows().getFirst().grade());
        assertEquals("VIP",report(5,5,5,"2026-10-02").rows().getFirst().grade());
    }
}
