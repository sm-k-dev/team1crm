package com.team1.crm.loyalty;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import com.team1.crm.loyalty.coupon.CouponForm;
import com.team1.crm.loyalty.coupon.CouponService;
import com.team1.crm.loyalty.visit.VisitForm;
import com.team1.crm.loyalty.visit.VisitResult;
import com.team1.crm.loyalty.visit.VisitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 방문 등록·쿠폰 발급 화면 컨트롤러의 요청 바인딩, 검증, 이동 주소, 화면에 전달하는 메시지를 검증한다.
 * 로그인 회원과 Service는 테스트용으로 대체한다. 보안 필터·DB·HTML 렌더링은 검증하지 않는다.
 * (렌더링은 LoyaltyTemplateTest가 확인한다.)
 *
 * 작성자: 이재준
 */
class LoyaltyViewControllerTest {

    private static final String DETAIL_URL = "/customers/100?storeId=900101";

    private MockMvc mvc;
    private StoreService stores;
    private VisitService visits;
    private CouponService coupons;
    private Member member;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        stores = mock(StoreService.class);
        visits = mock(VisitService.class);
        coupons = mock(CouponService.class);
        member = mock(Member.class);
        Store store = mock(Store.class);
        when(member.getId()).thenReturn(10L);
        when(store.getId()).thenReturn(900101L);
        when(stores.findOwnedStore(10L, 900101L)).thenReturn(store);
        when(stores.findOwnedStore(10L, 777L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = standaloneSetup(new LoyaltyViewController(stores, visits, coupons))
                .setValidator(validator)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(LoginMember.class)
                                && parameter.getParameterType() == Member.class;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return member; // null이면 로그인하지 않은 요청
                    }
                }).build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    private MockHttpServletRequestBuilder visitRequest(String storeId) {
        return post("/customers/100/visits").param("storeId", storeId);
    }

    private MockHttpServletRequestBuilder couponRequest(String storeId) {
        return post("/customers/100/coupons").param("storeId", storeId);
    }

    // ───────────── 방문 등록 ─────────────

    @Test
    @DisplayName("방문 등록에 성공하면 상세 화면으로 이동하고 결제 금액과 적립 포인트를 안내한다")
    void visitSuccessRedirectsWithMessage() throws Exception {
        when(visits.register(eq(900101L), eq(100L), any(VisitForm.class)))
                .thenReturn(new VisitResult(1L, 500, 0, 0, 10000));

        mvc.perform(visitRequest("900101").param("amount", "10000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DETAIL_URL))
                .andExpect(flash().attribute("visitMessage", "방문이 등록되었습니다. 결제 금액 10,000원, 적립 포인트 500P"));
    }

    @Test
    @DisplayName("쿠폰·포인트를 쓴 경우 결과 메시지에 할인과 사용 포인트를 함께 보여 준다")
    void visitMessageShowsCouponAndPoints() throws Exception {
        when(visits.register(eq(900101L), eq(100L), any(VisitForm.class)))
                .thenReturn(new VisitResult(1L, 700, 2000, 1000, 7000))      // 쿠폰 + 포인트
                .thenReturn(new VisitResult(2L, 800, 2000, 0, 8000))         // 쿠폰만
                .thenReturn(new VisitResult(3L, 900, 0, 1000, 9000));        // 포인트만

        mvc.perform(visitRequest("900101").param("amount", "10000"))
                .andExpect(flash().attribute("visitMessage",
                        "방문이 등록되었습니다. 결제 금액 7,000원 (쿠폰 할인 2,000원, 포인트 1,000P 사용), 적립 포인트 700P"));
        mvc.perform(visitRequest("900101").param("amount", "10000"))
                .andExpect(flash().attribute("visitMessage",
                        "방문이 등록되었습니다. 결제 금액 8,000원 (쿠폰 할인 2,000원), 적립 포인트 800P"));
        mvc.perform(visitRequest("900101").param("amount", "10000"))
                .andExpect(flash().attribute("visitMessage",
                        "방문이 등록되었습니다. 결제 금액 9,000원 (포인트 1,000P 사용), 적립 포인트 900P"));
    }

    @Test
    @DisplayName("폼 값(방문 일시, 쿠폰, 사용 포인트, 메모)이 VisitForm에 바인딩되고 빈 쿠폰 선택은 null이 된다")
    void visitFormBinding() throws Exception {
        when(visits.register(eq(900101L), eq(100L), any(VisitForm.class)))
                .thenReturn(new VisitResult(1L, 0, 0, 0, 0));

        mvc.perform(visitRequest("900101").param("amount", "5000").param("visitedAt", "2026-11-17T12:30")
                .param("couponId", "55").param("usedPoints", "300").param("menuMemo", "라떼"));
        mvc.perform(visitRequest("900101").param("amount", "5000").param("visitedAt", "").param("couponId", ""));

        ArgumentCaptor<VisitForm> form = ArgumentCaptor.forClass(VisitForm.class);
        verify(visits, times(2)).register(eq(900101L), eq(100L), form.capture());
        VisitForm first = form.getAllValues().get(0);
        assertEquals(5000, first.getAmount());
        assertEquals(LocalDateTime.of(2026, 11, 17, 12, 30), first.getVisitedAt());
        assertEquals(55L, first.getCouponId());
        assertEquals(300, first.getUsedPoints());
        assertEquals("라떼", first.getMenuMemo());
        VisitForm second = form.getAllValues().get(1);
        assertNull(second.getVisitedAt());
        assertNull(second.getCouponId());
    }

    @Test
    @DisplayName("입력값이 잘못되면 서비스를 호출하지 않고 안내 메시지와 함께 상세 화면으로 돌아간다")
    void invalidVisitInputIsRejectedBeforeService() throws Exception {
        for (String[] bad : new String[][]{
                {"amount", "-5"}, {"amount", ""}, {"amount", "100000001"}}) {
            mvc.perform(visitRequest("900101").param(bad[0], bad[1]))
                    .andExpect(redirectedUrl(DETAIL_URL))
                    .andExpect(flash().attribute("visitError", containsString("입력값을 확인해 주세요")));
        }
        mvc.perform(visitRequest("900101").param("amount", "1000").param("usedPoints", "-1"))
                .andExpect(flash().attribute("visitError", containsString("입력값을 확인해 주세요")));
        mvc.perform(visitRequest("900101").param("amount", "1000").param("menuMemo", "가".repeat(201)))
                .andExpect(flash().attribute("visitError", containsString("입력값을 확인해 주세요")));

        verifyNoInteractions(visits);
    }

    @Test
    @DisplayName("서비스가 거부하면(잔액 부족 등) 그 이유를 화면 안내로 보여 준다")
    void serviceRejectionBecomesVisitError() throws Exception {
        when(visits.register(eq(900101L), eq(100L), any(VisitForm.class)))
                .thenThrow(new IllegalArgumentException("사용 가능한 포인트가 부족합니다. 현재 잔액: 100P"));

        mvc.perform(visitRequest("900101").param("amount", "1000").param("usedPoints", "500"))
                .andExpect(redirectedUrl(DETAIL_URL))
                .andExpect(flash().attribute("visitError", "사용 가능한 포인트가 부족합니다. 현재 잔액: 100P"))
                .andExpect(flash().attributeCount(1)); // 성공 메시지는 함께 담기지 않는다
    }

    @Test
    @DisplayName("폐업 매장(409)은 오류 페이지가 아니라 화면 안내로 보여 준다")
    void closedStoreBecomesVisitError() throws Exception {
        when(visits.register(eq(900101L), eq(100L), any(VisitForm.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "폐업 매장은 변경할 수 없습니다."));

        mvc.perform(visitRequest("900101").param("amount", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DETAIL_URL))
                .andExpect(flash().attribute("visitError", "폐업 매장은 변경할 수 없습니다."));
    }

    // ───────────── 쿠폰 발급 ─────────────

    @Test
    @DisplayName("쿠폰 발급에 성공하면 상세 화면으로 이동하고 안내한다 (날짜 문자열이 LocalDate로 바인딩된다)")
    void couponIssueSuccess() throws Exception {
        mvc.perform(couponRequest("900101").param("title", "아메리카노").param("discount", "1500").param("expiresAt", "2026-12-31"))
                .andExpect(redirectedUrl(DETAIL_URL))
                .andExpect(flash().attribute("couponMessage", "쿠폰이 발급되었습니다."));

        ArgumentCaptor<CouponForm> form = ArgumentCaptor.forClass(CouponForm.class);
        verify(coupons).issue(eq(900101L), eq(100L), form.capture());
        assertEquals("아메리카노", form.getValue().getTitle());
        assertEquals(1500, form.getValue().getDiscount());
        assertEquals(LocalDate.of(2026, 12, 31), form.getValue().getExpiresAt());
    }

    @Test
    @DisplayName("쿠폰 입력값이 잘못되면 서비스를 호출하지 않고 안내한다")
    void invalidCouponInputIsRejectedBeforeService() throws Exception {
        mvc.perform(couponRequest("900101").param("title", "").param("discount", "1000").param("expiresAt", "2026-12-31"))
                .andExpect(flash().attribute("couponError", containsString("입력값을 확인해 주세요")));
        mvc.perform(couponRequest("900101").param("title", "쿠폰").param("discount", "0").param("expiresAt", "2026-12-31"))
                .andExpect(flash().attribute("couponError", containsString("입력값을 확인해 주세요")));
        mvc.perform(couponRequest("900101").param("title", "쿠폰").param("discount", "1000").param("expiresAt", ""))
                .andExpect(flash().attribute("couponError", containsString("입력값을 확인해 주세요")));

        verifyNoInteractions(coupons);
    }

    @Test
    @DisplayName("쿠폰 발급이 거부되면(과거 만료일, 폐업 매장) 이유를 화면 안내로 보여 준다")
    void couponIssueRejectionBecomesCouponError() throws Exception {
        doThrow(new IllegalArgumentException("만료일은 오늘 이후여야 합니다."))
                .doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "폐업 매장은 변경할 수 없습니다."))
                .when(coupons).issue(eq(900101L), eq(100L), any(CouponForm.class));

        mvc.perform(couponRequest("900101").param("title", "쿠폰").param("discount", "1000").param("expiresAt", "2020-01-01"))
                .andExpect(flash().attribute("couponError", "만료일은 오늘 이후여야 합니다."));
        mvc.perform(couponRequest("900101").param("title", "쿠폰").param("discount", "1000").param("expiresAt", "2026-12-31"))
                .andExpect(flash().attribute("couponError", "폐업 매장은 변경할 수 없습니다."));
    }

    // ───────────── 접근 제어 ─────────────

    @Test
    @DisplayName("로그인하지 않았으면 로그인 화면으로 보내고 서비스를 호출하지 않는다")
    void notLoggedInRedirectsToLogin() throws Exception {
        member = null;

        mvc.perform(visitRequest("900101").param("amount", "1000"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(couponRequest("900101").param("title", "쿠폰").param("discount", "1000").param("expiresAt", "2026-12-31"))
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(visits, coupons, stores);
    }

    @Test
    @DisplayName("내 매장이 아니면 403이고 서비스를 호출하지 않는다")
    void notMyStoreIsForbidden() throws Exception {
        mvc.perform(visitRequest("777").param("amount", "1000")).andExpect(status().isForbidden());
        mvc.perform(couponRequest("777").param("title", "쿠폰").param("discount", "1000").param("expiresAt", "2026-12-31"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(visits, coupons);
    }
}
