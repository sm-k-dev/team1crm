package com.team1.crm.auth;

import com.team1.crm.customer.*;
import com.team1.crm.home.HomeSummaryService;
import com.team1.crm.loyalty.visit.Visit;
import com.team1.crm.loyalty.point.*;
import com.team1.crm.loyalty.coupon.Coupon;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** 폐업의 소유권·정보 제거·거래 보존과 쓰기 차단을 검증한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-closure-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import({StoreClosureService.class, CustomerService.class, HomeSummaryService.class})
class StoreClosureTest {
    @Autowired EntityManager em;
    @Autowired StoreClosureService closure;
    @Autowired CustomerService customers;
    @Autowired HomeSummaryService summaries;

    /** 주요 정보와 자유 입력만 제거하고 이름·거래·다른 매장을 보존한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("폐업은 매장 이름·거래·통계를 보존하고 해당 매장 고객 정보만 즉시 제거한다")
    void closesWithoutChangingStoreNameOrHistoricalTotals() {
        var owner = owner("one"); var store = store(owner, "원래 매장 이름"); var other = store(owner, "영업 유지");
        var customer = customer(store, "고객 이름", "010-0000-1111");
        var otherCustomer = customer(other, "다른 매장 고객", "010-0000-2222");
        var time = LocalDateTime.of(2026, 11, 11, 12, 0);
        var visit = new Visit(customer, time, 30000, 0, "고객 연락처 메모"); em.persist(visit);
        var point = new PointHistory(customer, visit, PointType.EARN, 1500, time); em.persist(point);
        var coupon = new Coupon(customer, "개인정보 가능 제목", 1000, LocalDate.of(2027, 1, 1)); em.persist(coupon);
        em.flush();
        var before = summaries.summarize(store.getId(), time.toLocalDate());
        closure.close(owner.getId(), store.getId()); em.flush(); em.clear();
        Store saved = em.find(Store.class, store.getId());
        assertEquals("원래 매장 이름", saved.getName());
        assertEquals(StoreStatus.CLOSED, saved.getStatus()); assertNotNull(saved.getClosedAt());
        Customer scrubbed = em.find(Customer.class, customer.getId());
        assertEquals("삭제된 고객", scrubbed.getName()); assertNull(scrubbed.getPhone());
        assertNull(scrubbed.getBirthMonth()); assertNull(scrubbed.getMemo()); assertFalse(scrubbed.isAgreeMarketing());
        assertNotNull(scrubbed.getDeletedAt()); assertNotNull(scrubbed.getAnonymizedAt());
        assertEquals("다른 매장 고객", em.find(Customer.class, otherCustomer.getId()).getName());
        assertFalse(em.find(Store.class, other.getId()).isClosed());
        var after = summaries.summarize(store.getId(), time.toLocalDate());
        assertEquals(0, after.customerCount()); assertEquals(before.todayVisits(), after.todayVisits());
        assertEquals(before.monthRevenue(), after.monthRevenue()); assertEquals(30000, after.monthRevenue());
        assertNull(em.find(Visit.class, visit.getId()).getMenuMemo());
        assertEquals(1500, em.find(PointHistory.class, point.getId()).getPoint());
        assertEquals(1000, em.find(Coupon.class, coupon.getId()).getDiscount());
        assertEquals("폐업 매장 쿠폰", em.find(Coupon.class, coupon.getId()).getTitle());
        var closedAt = saved.getClosedAt(); var anonymizedAt = scrubbed.getAnonymizedAt();
        closure.close(owner.getId(), store.getId()); em.flush(); em.clear();
        assertEquals(closedAt, em.find(Store.class, store.getId()).getClosedAt());
        assertEquals(anonymizedAt, em.find(Customer.class, customer.getId()).getAnonymizedAt());
    }

    /** 다른 소유자의 요청은 데이터를 변경하지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("다른 사장님의 폐업 요청은 403이고 상태와 고객 정보는 유지된다")
    void rejectsOtherOwner() {
        var owner = owner("real"); var stranger = owner("stranger"); var store = store(owner, "보호 매장");
        var customer = customer(store, "보존 고객", "010-0000-3333");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> closure.close(stranger.getId(), store.getId())).getStatusCode().value());
        assertFalse(store.isClosed()); assertEquals("보존 고객", customer.getName());
    }

    /** 폐업 매장은 새 고객 저장·관리자 변경을 허용하지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("폐업 매장의 고객 등록·수정·삭제와 매장 정보 변경은 409로 차단한다")
    void rejectsWritesAfterClosure() {
        var owner = owner("blocked"); var store = store(owner, "읽기 전용");
        var customer = customer(store, "폐업 전 고객", "010-0000-4444");
        closure.close(owner.getId(), store.getId()); em.flush(); em.clear();
        var form = new CustomerForm(); form.setName("신규"); form.setPhone("010-1234-5678");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> customers.registerCustomer(store, form)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> customers.updateCustomer(store.getId(), customer.getId(), form));
        assertThrows(ResponseStatusException.class, () -> customers.deleteCustomer(store.getId(), customer.getId()));
        assertThrows(ResponseStatusException.class, () -> em.find(Store.class, store.getId()).update("다른 이름", "업종", "주소", 5));
    }

    /** 실제 화면에 상태·확인 문구·통계 링크를 표시한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("내 매장 화면은 원래 이름과 폐업 상태·통계 링크를 표시하고 폐업 버튼을 숨긴다")
    void rendersClosedStoreAndEmptyGuidance() {
        var owner = owner("view"); var store = store(owner, "이름 유지 검증");
        var model = new java.util.HashMap<String, Object>(); model.put("stores", java.util.List.of(store));
        assertTrue(render("auth/store-settings", model, owner).contains("폐업 처리"));
        closure.close(owner.getId(), store.getId()); em.flush(); em.clear();
        model.put("stores", java.util.List.of(em.find(Store.class, store.getId())));
        String html = render("auth/store-settings", model, owner);
        assertTrue(html.contains("이름 유지 검증")); assertTrue(html.contains("폐업"));
        assertTrue(html.contains("과거 통계 조회")); assertFalse(html.contains(">폐업 처리</button>"));
        model.clear(); model.put("closedOnly", false);
        assertTrue(render("customer/no-store", model, owner).contains("연결된 매장이 없습니다."));
        model.put("closedOnly", true); model.put("closedStoreId", store.getId());
        assertTrue(render("customer/no-store", model, owner).contains("과거 통계 보기"));
        model.clear(); model.put("selectedStore", em.find(Store.class, store.getId()));
        model.put("stores", java.util.List.of(em.find(Store.class, store.getId())));
        model.put("selectedMonth", "2026-11");
        assertTrue(render("analytics/dashboard", model, owner).contains("폐업 매장은 고객 등급 분석을 제공하지 않습니다."));
    }

    /** 미로그인과 관리자 요청을 폐업 처리 전에 거절한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("폐업 요청 Controller는 USER 사장님만 허용한다")
    void controllerRequiresOwnerRole() {
        var controller = new StoreClosureController(closure);
        var redirect = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        assertEquals("redirect:/login", controller.close(null, 1L, redirect));
        var admin = new Member("admin-close@test.invalid", "unused", "관리자", Role.ADMIN);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.close(admin, 1L, redirect)).getStatusCode().value());
    }

    /** 실제 템플릿으로 화면의 읽기 전용 안내를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private String render(String template, java.util.Map<String, Object> model, Member owner) {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI("/store-settings");
        var web = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet);
        var context = new org.thymeleaf.context.WebContext(web.buildExchange(request,
                new org.springframework.mock.web.MockHttpServletResponse()), java.util.Locale.KOREAN);
        context.setVariables(model); context.setVariable("loginMember", owner);
        return engine.process(template, context);
    }

    /** 검증용 소유자를 만든다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Member owner(String suffix) {
        var member = new Member(suffix + "@test.invalid", "unused", suffix, Role.USER); em.persist(member); return member;
    }
    /** 검증용 매장을 만든다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Store store(Member owner, String name) { var store = new Store(owner, name, "카페", "서울", 5); em.persist(store); return store; }
    /** 민감 정보가 있는 고객을 만든다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Customer customer(Store store, String name, String phone) {
        var customer = new Customer(store, name, phone, 11, "고객 메모", true, null); em.persist(customer); return customer;
    }
}
