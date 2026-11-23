package com.team1.crm.auth;

import com.team1.crm.home.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 등록 위치 이동·CSRF 전달·영업 상태 정렬을 검증한다. 작성자: 김순민 / 작성일: 2026-11-12 */
class StoreManagementLayoutTest {
    /** 등록 폼과 보안 토큰이 함께 이동했는지 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("매장 관리는 등록 폼·CSRF·등록 스크립트를 포함하고 홈은 관리 링크만 제공한다")
    void registrationMovesWithSecurityMetadata() {
        var member = new Member("layout@test.invalid", "unused", "사장님", Role.USER);
        var model = new ExtendedModelMap(); model.addAttribute("stores", List.of());
        model.addAttribute("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-csrf-value"));
        String settings = render("auth/store-settings", model, member);
        assertTrue(settings.contains("id=\"storeForm\""));
        assertTrue(settings.contains("content=\"test-csrf-value\""));
        assertTrue(settings.contains("content=\"X-CSRF-TOKEN\""));
        assertTrue(settings.contains("/js/store-registration.js"));
        assertTrue(settings.contains("href=\"#storeRegistration\""));
        String home = render("home/index", model, member);
        assertFalse(home.contains("id=\"storeForm\""));
        assertFalse(home.contains("/js/store-registration.js"));
        assertTrue(home.contains("/store-settings#storeRegistration"));
        assertTrue(home.contains("href=\"/store-settings\""));
        model.addAttribute("closedOnly", false);
        assertTrue(render("customer/no-store", model, member).contains("/store-settings#storeRegistration"));
    }

    /** 정렬된 목록과 화면 구역을 실제로 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("내 매장은 영업 중 ID순 다음 폐업 ID순으로 구역을 나눠 표시한다")
    void openStoresComeFirstInIdOrder() {
        var stores = mock(StoreService.class);
        var openHigh = sample(9L, false); var closedLow = sample(1L, true);
        var openLow = sample(3L, false); var closedHigh = sample(7L, true);
        when(stores.getMyStores("layout@test.invalid")).thenReturn(List.of(closedHigh, openHigh, closedLow, openLow));
        var auth = new UsernamePasswordAuthenticationToken("layout@test.invalid", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var model = new ExtendedModelMap();
        assertEquals("auth/store-settings", new StoreSettingsViewController(stores).storeSettings(auth, model));
        assertEquals(List.of(openLow, openHigh, closedLow, closedHigh), model.get("stores"));
        String html = render("auth/store-settings", model, new Member("layout@test.invalid", "unused", "사장님", Role.USER));
        assertTrue(html.indexOf("영업 중인 매장") < html.indexOf("폐업한 매장"));
        assertTrue(html.indexOf("매장3") < html.indexOf("매장9"));
        assertTrue(html.indexOf("매장9") < html.indexOf("매장1"));
        assertTrue(html.indexOf("매장1") < html.indexOf("매장7"));
        assertTrue(html.indexOf("현황 · 분석") < html.indexOf("매장 · 고객 관리"));
        assertTrue(html.indexOf("data-route=\"/analytics/dashboard\"") < html.indexOf("data-route=\"/store-settings\""));
    }

    /** 식별자와 상태가 다른 매장 대역을 만든다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private Store sample(Long id, boolean closed) {
        Store store = mock(Store.class); when(store.getId()).thenReturn(id); when(store.getName()).thenReturn("매장" + id);
        when(store.isClosed()).thenReturn(closed); when(store.getStatus()).thenReturn(closed ? StoreStatus.CLOSED : StoreStatus.OPEN);
        return store;
    }

    /** 실제 템플릿을 렌더링한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private String render(String template, ExtendedModelMap model, Member member) {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI("/store-settings");
        var web = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet);
        var context = new org.thymeleaf.context.WebContext(web.buildExchange(request,
                new org.springframework.mock.web.MockHttpServletResponse()), Locale.KOREAN);
        context.setVariables(model); context.setVariable("loginMember", member);
        return engine.process(template, context);
    }
}
