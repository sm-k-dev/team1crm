package com.team1.crm.home;

import com.team1.crm.auth.*;
import com.team1.crm.customer.Customer;
import com.team1.crm.loyalty.visit.Visit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 홈 집계의 실제 DB 조건과 선택 매장 권한·화면을 검사한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:home-summary-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import(HomeSummaryService.class)
class HomeSummaryTest {
    @Autowired EntityManager em;
    @Autowired HomeSummaryService summary;

    /** 삭제 고객 이력과 월·일 경계, 타 매장 제외를 검증한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("홈은 활성 고객만 세고 삭제 고객의 방문·매출은 보존하며 매장을 격리한다")
    void aggregatesWithBoundariesAndDeletedCustomers() {
        var owner = new Member("home@test.invalid", "unused", "홈 사장님", Role.USER); em.persist(owner);
        var store = new Store(owner, "검증 매장", "카페", "서울", 5); em.persist(store);
        var other = new Store(owner, "다른 매장", "카페", "서울", 5); em.persist(other);
        var active = customer(store, "활성", "010-0000-1111");
        var deleted = customer(store, "삭제", "010-0000-2222"); deleted.softDelete();
        var outside = customer(other, "타 매장", "010-0000-3333");
        visit(active, "2026-10-31T23:59:59", 100);
        visit(active, "2026-11-01T00:00:00", 1000);
        visit(deleted, "2026-11-11T00:00:00", 2000);
        visit(active, "2026-11-11T23:59:59", 3000);
        visit(active, "2026-11-12T00:00:00", 4000);
        visit(active, "2026-12-01T00:00:00", 5000);
        visit(outside, "2026-11-11T12:00:00", 99999);
        em.flush(); em.clear();
        assertEquals(new HomeSummary(1, 2, 10000), summary.summarize(store.getId(), LocalDate.of(2026, 11, 11)));
    }

    /** 데이터가 없으면 세 집계를 모두 0으로 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("기록 없는 매장의 홈 집계는 모두 0이다")
    void emptySummaryIsZero() {
        assertEquals(new HomeSummary(0, 0, 0), summary.summarize(Long.MAX_VALUE, LocalDate.of(2026, 1, 1)));
    }

    /** 매장 변경 요청은 소유 목록에서만 선택한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("홈의 선택 매장에 집계를 연결하고 다른 매장 ID는 403으로 거절한다")
    void selectsOnlyOwnedStores() {
        var stores = mock(StoreService.class);
        var totals = mock(HomeSummaryService.class);
        var first = mock(Store.class); when(first.getId()).thenReturn(1L);
        var second = mock(Store.class); when(second.getId()).thenReturn(2L); when(second.getName()).thenReturn("선택 매장");
        when(stores.getMyStores("home@test.invalid")).thenReturn(List.of(first, second));
        when(totals.summarize(eq(2L), any())).thenReturn(new HomeSummary(3, 4, 5000));
        var controller = new HomeController(stores, totals);
        var auth = new UsernamePasswordAuthenticationToken("home@test.invalid", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var model = new ExtendedModelMap();
        assertEquals("home/index", controller.home(auth, 2L, model));
        assertSame(second, model.get("selectedStore"));
        assertEquals(new HomeSummary(3, 4, 5000), model.get("summary"));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.home(auth, 999L, new ExtendedModelMap())).getStatusCode().value());
        verify(totals, never()).summarize(eq(999L), any());
        String html = render(model);
        assertTrue(html.contains("3명"));
        assertTrue(html.contains("4건"));
        assertTrue(html.contains("5,000원"));
        assertTrue(html.contains("/customers?storeId=2"));
        assertTrue(html.contains("name=\"storeId\""));
    }

    /** 관리자·미로그인·매장 미보유 계정의 기본 처리를 검사한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("홈은 관리자 대시보드 이동과 미로그인·매장 없는 화면을 처리한다")
    void handlesAdminAndNoStore() {
        var stores = mock(StoreService.class); var totals = mock(HomeSummaryService.class);
        var controller = new HomeController(stores, totals);
        assertEquals("home/index", controller.home(null, null, new ExtendedModelMap()));
        var admin = new UsernamePasswordAuthenticationToken("admin@test.invalid", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertEquals("redirect:/admin/dashboard", controller.home(admin, null, new ExtendedModelMap()));
        var user = new UsernamePasswordAuthenticationToken("empty@test.invalid", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(stores.getMyStores("empty@test.invalid")).thenReturn(List.of());
        var model = new ExtendedModelMap(); controller.home(user, null, model);
        assertTrue(render(model).contains("등록된 매장이 없습니다."));
        verifyNoInteractions(totals);
    }

    /** 검증용 고객을 저장한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Customer customer(Store store, String name, String phone) {
        var customer = new Customer(store, name, phone, null, null, false, null); em.persist(customer); return customer;
    }

    /** 지정한 경계 시각의 방문을 저장한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private void visit(Customer customer, String date, int amount) {
        em.persist(new Visit(customer, LocalDateTime.parse(date), amount, 0, "홈 검증"));
    }

    /** 실제 홈 템플릿의 집계 출력과 이동 링크를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private String render(ExtendedModelMap model) {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI("/");
        var application = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet);
        var context = new org.thymeleaf.context.WebContext(application.buildExchange(request,
                new org.springframework.mock.web.MockHttpServletResponse()), Locale.KOREAN);
        context.setVariables(model);
        context.setVariable("loginMember", new Member("home@test.invalid", "unused", "사장님", Role.USER));
        return engine.process("home/index", context);
    }
}
