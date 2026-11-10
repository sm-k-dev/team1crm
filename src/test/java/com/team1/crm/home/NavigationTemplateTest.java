package com.team1.crm.home;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 공통 내비게이션을 렌더링하여 역할별 메뉴와 로그인 표시를 검증한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-10
 */
class NavigationTemplateTest {
    private SpringTemplateEngine engine;
    private WebContext context;

    /**
     * 실제 템플릿을 처리할 엔진과 테스트 웹 요청을 준비한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setRequestURI("/customers");
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        context = new WebContext(
                application.buildExchange(request, new MockHttpServletResponse()),
                Locale.KOREAN
        );
    }

    /**
     * 관리자는 관리자 메뉴와 로그아웃만 표시하고 사장님 메뉴는 제외한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("ADMIN은 관리자 메뉴만 표시한다")
    void adminOnlySeesAdminMenu() {
        context.setVariable("loginMember", new Member("admin@test.invalid", "unused", "관리자", Role.ADMIN));
        String html = engine.process("fragments/navigation", context);
        assertAll(
                () -> assertTrue(html.contains("href=\"/admin/stores\"")),
                () -> assertTrue(html.contains("href=\"/admin/owners\"")),
                () -> assertFalse(html.contains("href=\"/customers\"")),
                () -> assertFalse(html.contains("href=\"/store-settings\"")),
                () -> assertFalse(html.contains("href=\"/analytics/dashboard\"")),
                () -> assertFalse(html.contains("href=\"/login\"")),
                () -> assertTrue(html.contains("action=\"/logout\"")),
                () -> assertTrue(html.contains("method=\"post\""))
        );
    }

    /**
     * 현재 사장님 역할인 USER는 매장 운영 메뉴만 표시한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("사장님 USER는 매장 운영 메뉴만 표시한다")
    void ownerOnlySeesWorkspaceMenu() {
        context.setVariable("loginMember", new Member("owner@test.invalid", "unused", "사장님", Role.USER));
        String html = engine.process("fragments/navigation", context);
        assertAll(
                () -> assertTrue(html.contains("href=\"/customers\"")),
                () -> assertTrue(html.contains("href=\"/store-settings\"")),
                () -> assertTrue(html.contains("href=\"/analytics/dashboard\"")),
                () -> assertFalse(html.contains("href=\"/admin/")),
                () -> assertFalse(html.contains("href=\"/signup\"")),
                () -> assertTrue(html.contains("action=\"/logout\""))
        );
    }

    /**
     * 비로그인 방문자는 로그인·회원가입만 표시한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("비로그인은 로그인·회원가입만 표시한다")
    void anonymousOnlySeesAccountMenu() {
        String html = engine.process("fragments/navigation", context);
        assertAll(
                () -> assertTrue(html.contains("href=\"/login\"")),
                () -> assertTrue(html.contains("href=\"/signup\"")),
                () -> assertFalse(html.contains("href=\"/admin/")),
                () -> assertFalse(html.contains("href=\"/customers\"")),
                () -> assertFalse(html.contains("action=\"/logout\""))
        );
    }
}
