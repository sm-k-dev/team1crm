package com.team1.crm.loyalty;

import com.team1.crm.loyalty.coupon.Coupon;
import com.team1.crm.loyalty.point.PointHistory;
import com.team1.crm.loyalty.point.PointType;
import com.team1.crm.loyalty.visit.Visit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * loyalty 화면 조각(방문·쿠폰·포인트)을 실제 Thymeleaf로 렌더링해서 화면에 나오는 문구와 폼 주소를 검증한다.
 * 상세 화면 전체(customer/detail)가 아니라 조각만 렌더링하므로, 조각이 요구하는 모델 값을 직접 넣는다.
 *
 * 작성자: 이재준
 */
class LoyaltyTemplateTest {

    private SpringTemplateEngine engine;
    private WebContext context;

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
        request.setRequestURI("/customers/100");
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        context = new WebContext(application.buildExchange(request, new MockHttpServletResponse()), Locale.KOREAN);

        // 조각이 필요로 하는 공통 모델 값 (CustomerViewController.detail이 넣어 주는 값과 같은 이름)
        context.setVariable("customerId", 100L);
        context.setVariable("selectedStore", new StoreView(900101L));
        context.setVariable("pointBalance", 1305L);
    }

    /** selectedStore.id 만 필요하므로 엔티티 대신 같은 이름의 값을 가진 단순 객체를 쓴다. */
    public record StoreView(Long id) {
        public Long getId() { return id; }
    }

    private String render(String template, String fragment) {
        return engine.process(template, Set.of(fragment), context);
    }

    private Coupon coupon(Long id, String title, int discount, LocalDate expiresAt, LocalDateTime usedAt) {
        Coupon coupon = new Coupon(null, title, discount, expiresAt);
        ReflectionTestUtils.setField(coupon, "id", id);
        ReflectionTestUtils.setField(coupon, "usedAt", usedAt);
        return coupon;
    }

    // ───────────── 방문 등록 영역 ─────────────

    @Test
    @DisplayName("방문 등록 폼은 고객·매장 번호가 들어간 주소로 제출하고 입력칸을 모두 보여 준다")
    void visitPanelHasFormAndFields() {
        context.setVariable("coupons", List.of());

        String html = render("loyalty/_visit", "visitPanel");

        assertAll(
                () -> assertTrue(html.contains("action=\"/customers/100/visits?storeId=900101\"")),
                () -> assertTrue(html.contains("method=\"post\"")),
                () -> assertTrue(html.contains("name=\"amount\"")),
                () -> assertTrue(html.contains("name=\"visitedAt\"")),
                () -> assertTrue(html.contains("name=\"couponId\"")),
                () -> assertTrue(html.contains("name=\"usedPoints\"")),
                () -> assertTrue(html.contains("name=\"menuMemo\"")),
                () -> assertTrue(html.contains("개인정보는 적지 마세요")),          // 방문 메모 입력 안내 (B안)
                () -> assertTrue(html.contains("이 메모도 함께 삭제됩니다")),
                () -> assertTrue(html.contains("보유 <span>1,305</span>P")),
                () -> assertTrue(html.contains("max=\"1305\"")),   // 보유 포인트를 넘게 입력하지 못하게 화면에서도 제한
                () -> assertFalse(html.contains("alert alert"))     // 메시지가 없으면 알림 영역도 없다
        );
    }

    @Test
    @DisplayName("쿠폰 선택칸에는 사용 가능한 쿠폰만 나온다 (사용했거나 기한이 지난 쿠폰은 제외)")
    void visitPanelListsOnlyUsableCoupons() {
        context.setVariable("coupons", List.of(
                coupon(1L, "사용가능쿠폰", 2000, LocalDate.now().plusDays(3), null),
                coupon(2L, "사용한쿠폰", 1000, LocalDate.now().plusDays(3), LocalDateTime.now()),
                coupon(3L, "만료쿠폰", 1000, LocalDate.now().minusDays(1), null)));

        String html = render("loyalty/_visit", "visitPanel");

        assertAll(
                () -> assertTrue(html.contains("value=\"1\"")),
                () -> assertTrue(html.contains("사용가능쿠폰 (2,000원 할인)")),
                () -> assertFalse(html.contains("사용한쿠폰")),
                () -> assertFalse(html.contains("만료쿠폰")),
                () -> assertTrue(html.contains("사용 안 함"))
        );
    }

    @Test
    @DisplayName("성공·오류 메시지는 있을 때만 알림으로 보여 준다")
    void visitPanelShowsMessages() {
        context.setVariable("coupons", List.of());
        context.setVariable("visitMessage", "방문이 등록되었습니다. 결제 금액 7,000원");

        String success = render("loyalty/_visit", "visitPanel");
        assertTrue(success.contains("alert alert-success"));
        assertTrue(success.contains("방문이 등록되었습니다. 결제 금액 7,000원"));
        assertFalse(success.contains("alert-danger"));

        context.setVariable("visitMessage", null);
        context.setVariable("visitError", "사용 가능한 포인트가 부족합니다. 현재 잔액: 100P");
        String error = render("loyalty/_visit", "visitPanel");
        assertTrue(error.contains("alert alert-danger"));
        assertTrue(error.contains("사용 가능한 포인트가 부족합니다. 현재 잔액: 100P"));
        assertFalse(error.contains("alert-success"));
    }

    @Test
    @DisplayName("최근 방문 목록은 일시·메모·결제 금액(천 단위 쉼표)을 보여 주고, 없으면 안내 문구를 보여 준다")
    void visitHistory() {
        Visit visit = new Visit(null, LocalDateTime.of(2026, 11, 17, 12, 30), 12000, 0, "라떼");
        context.setVariable("visits", List.of(visit));
        String html = render("loyalty/_visit", "visitHistory");
        assertAll(
                () -> assertTrue(html.contains("2026-11-17 12:30")),
                () -> assertTrue(html.contains("라떼")),
                () -> assertTrue(html.contains("12,000원")),
                () -> assertFalse(html.contains("방문 기록이 없습니다"))
        );

        context.setVariable("visits", List.of());
        assertTrue(render("loyalty/_visit", "visitHistory").contains("방문 기록이 없습니다"));
    }

    // ───────────── 쿠폰 영역 ─────────────

    @Test
    @DisplayName("쿠폰 발급 폼과 목록(상태 배지)을 보여 주고, 단독 사용 버튼은 없다")
    void couponPanel() {
        context.setVariable("coupons", List.of(
                coupon(1L, "아메리카노 1잔", 3000, LocalDate.now().plusDays(3), null),
                coupon(2L, "사용한쿠폰", 1000, LocalDate.now().plusDays(3), LocalDateTime.now())));

        String html = render("loyalty/_coupon", "couponPanel");

        assertAll(
                () -> assertTrue(html.contains("action=\"/customers/100/coupons?storeId=900101\"")),
                () -> assertTrue(html.contains("name=\"title\"")),
                () -> assertTrue(html.contains("name=\"discount\"")),
                () -> assertTrue(html.contains("name=\"expiresAt\"")),
                () -> assertTrue(html.contains("아메리카노 1잔")),
                () -> assertTrue(html.contains("3,000원 할인")),
                () -> assertTrue(html.contains("사용 가능")),
                () -> assertTrue(html.contains("사용함")),
                () -> assertFalse(html.contains("/redeem")),          // 쿠폰은 방문 등록에서만 사용한다
                () -> assertTrue(html.contains("방문 등록</strong>에서 선택해 사용")),
                () -> assertTrue(html.contains("개인정보를 적지 마세요")),          // 쿠폰 제목 입력 안내 (B안)
                () -> assertTrue(html.contains("삭제된 고객 쿠폰"))
        );
    }

    @Test
    @DisplayName("쿠폰이 없으면 안내 문구를, 오류 메시지가 있으면 알림을 보여 준다")
    void couponPanelEmptyAndError() {
        context.setVariable("coupons", List.of());
        context.setVariable("couponError", "만료일은 오늘 이후여야 합니다.");

        String html = render("loyalty/_coupon", "couponPanel");

        assertTrue(html.contains("쿠폰이 없습니다"));
        assertTrue(html.contains("alert alert-danger"));
        assertTrue(html.contains("만료일은 오늘 이후여야 합니다."));
    }

    // ───────────── 포인트 영역 ─────────────

    @Test
    @DisplayName("포인트 영역은 잔액과 내역(적립·사용 구분, 포인트 쉼표)을 보여 준다")
    void pointPanel() {
        PointHistory earn = new PointHistory(null, null, PointType.EARN, 1500, LocalDateTime.of(2026, 11, 17, 12, 30));
        PointHistory use = new PointHistory(null, null, PointType.USE, 300, LocalDateTime.of(2026, 11, 17, 13, 0));
        context.setVariable("pointHistory", List.of(use, earn));

        String html = render("loyalty/_point", "pointPanel");

        assertAll(
                () -> assertTrue(html.contains("<strong>1,305</strong>")),
                () -> assertTrue(html.contains("2026-11-17 12:30")),
                () -> assertTrue(html.contains("적립")),
                () -> assertTrue(html.contains("사용")),
                () -> assertTrue(html.contains("1,500")),
                () -> assertFalse(html.contains("EARN")),             // 영문 코드가 그대로 보이지 않는다
                () -> assertFalse(html.contains("포인트 내역이 없습니다"))
        );

        context.setVariable("pointHistory", List.of());
        assertTrue(render("loyalty/_point", "pointPanel").contains("포인트 내역이 없습니다"));
    }
}
