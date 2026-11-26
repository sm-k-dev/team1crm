package com.team1.crm.analytics;

import com.team1.crm.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.YearMonth;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 분석 화면의 실제 Controller 호출로 소유 범위를 재검증한다. 김순민, 2026-11-27 */
class DashboardAccessTest {
    final AnalyticsService analytics = mock(AnalyticsService.class);
    final StoreService stores = mock(StoreService.class);
    final DashboardViewController controller = new DashboardViewController(analytics, stores);
    final Member owner = new Member("owner@test.invalid", "unused", "검증", Role.USER);
    final YearMonth month = YearMonth.of(2026, 11);

    Store store(long id) {
        Store s = mock(Store.class); when(s.getId()).thenReturn(id); return s;
    }

    @Test void rejectsForeignStoreBeforeAnyAnalyticsQuery() {
        var owned = store(1);
        when(stores.getMyStores(owner.getEmail())).thenReturn(List.of(owned));
        var error = assertThrows(ResponseStatusException.class,
                () -> controller.dashboard(month, 2L, owner, new ExtendedModelMap()));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(analytics);
    }

    @Test void selectsOnlyOwnedStoreAndPassesItsReportToView() {
        var high = store(9); var low = store(3);
        when(stores.getMyStores(owner.getEmail())).thenReturn(List.of(high, low));
        var summary = new AnalyticsService.Summary(30000, 3, 2, 1, new long[7], new long[24], List.of());
        var rfm = new AnalyticsService.RfmReport(List.of(), List.of(new AnalyticsService.GradeCount("VIP", 0)));
        when(analytics.summarize(3L, month)).thenReturn(summary);
        when(analytics.summarizeRfm(3L, month)).thenReturn(rfm);
        var model = new ExtendedModelMap();
        assertEquals("analytics/dashboard", controller.dashboard(month, null, owner, model));
        assertEquals(List.of(low, high), model.get("stores"));
        assertSame(low, model.get("selectedStore"));
        assertSame(rfm, model.get("rfm"));
        assertEquals(50L, model.get("repeatRate"));
        verify(analytics).summarize(3L, month);
        verify(analytics).summarizeRfm(3L, month);
        verifyNoMoreInteractions(analytics);
    }

    @Test void unauthenticatedRequestDoesNotQueryData() {
        var error = assertThrows(ResponseStatusException.class,
                () -> controller.dashboard(month, null, null, new ExtendedModelMap()));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(stores, analytics);
    }
}
