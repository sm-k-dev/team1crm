package com.team1.crm.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 차트 입력표와 RFM 행의 실제 Thymeleaf 렌더링을 검증한다. 김순민, 2026-11-27 */
class DashboardTemplateTest {
    String render(AnalyticsService.RfmReport rfm) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
        request.setRequestURI("/analytics/dashboard");
        var web = JakartaServletWebApplication.buildApplication(servlet);
        var context = new WebContext(web.buildExchange(request,new MockHttpServletResponse()),Locale.KOREAN);
        context.setVariable("stores",List.of()); context.setVariable("selectedMonth","2026-11");
        context.setVariable("rfm",rfm);
        if (!rfm.rows().isEmpty()) {
            var store=org.mockito.Mockito.mock(com.team1.crm.auth.Store.class);
            org.mockito.Mockito.when(store.getId()).thenReturn(1L);
            org.mockito.Mockito.when(store.getName()).thenReturn("검증매장");
            context.setVariable("stores",List.of(store));
            context.setVariable("selectedStore",store);
        }
        return engine.process("analytics/dashboard",context);
    }

    @Test void rendersRealGradeCountsAndCustomerRowsInsteadOfExamples() {
        var report = new AnalyticsService.RfmReport(
                List.of(new AnalyticsService.RfmRow("검증고객", "2026-11-20", 3, 30000, 5, 4, 3, "단골")),
                List.of(new AnalyticsService.GradeCount("VIP",0),new AnalyticsService.GradeCount("단골",1),
                        new AnalyticsService.GradeCount("일반",0),new AnalyticsService.GradeCount("이탈 위험",0)));
        String html=render(report);
        assertTrue(html.contains("id=\"gradeChart\""));
        assertTrue(html.contains("id=\"rfmGradeCounts\""));
        assertTrue(html.contains("검증고객"));
        assertTrue(html.contains("data-rfm-grade=\"단골\""));
        assertTrue(html.contains("data-amount=\"30000\""));
        assertTrue(java.util.regex.Pattern.compile("<th[^>]*>단골</th>\\s*<td[^>]*>1</td>").matcher(html).find());
        assertTrue(java.util.regex.Pattern.compile("<th[^>]*>VIP</th>\\s*<td[^>]*>0</td>").matcher(html).find());
        assertFalse(html.contains("선택한 월에 등급 분석 대상 고객이 없습니다."));
    }

    @Test void emptyReportShowsGuidanceWithoutChartOrFakeCustomer() {
        String html=render(new AnalyticsService.RfmReport(List.of(),List.of()));
        assertTrue(html.contains("선택한 월에 등급 분석 대상 고객이 없습니다."));
        assertFalse(html.contains("id=\"gradeChart\""));
        assertFalse(html.contains("data-rfm-grade="));
    }
}
