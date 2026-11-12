package com.team1.crm.customer;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 고객 화면 Controller의 요청 바인딩·검증·상태 코드·화면 및 이동 주소를 검증한다.
 * 로그인 회원과 Service는 테스트용으로 대체한다. 보안 필터·DB·HTML 렌더링은 검증하지 않는다.
 * 작성자: 김순민
 * 작성일: 2026-11-11
 */
class CustomerViewControllerTest {
    private MockMvc mvc;
    private CustomerService customers;
    private StoreService stores;
    private Member member;
    private Store store;
    private Customer customer;
    private LocalValidatorFactoryBean validator;

    /** 삭제 요청은 소유 매장의 고객을 소프트 삭제한 후 목록으로 이동한다.
     * 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("고객 삭제 성공 시 선택 매장 목록으로 이동한다")
    void deleteRedirectsToList() throws Exception {
        mvc.perform(request("delete")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers?storeId=900101"));
        verify(customers).deleteCustomer(900101L, 100L);
    }

    /** 없거나 이미 삭제된 고객은 404로 처리한다.
     * 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("고객 삭제 대상이 없으면 404를 반환한다")
    void missingDeleteReturns404() throws Exception {
        doThrow(new IllegalArgumentException("고객 없음")).when(customers).deleteCustomer(900101L, 100L);
        mvc.perform(request("delete")).andExpect(status().isNotFound());
    }

    /** 테스트별 객체·검증 도구·로그인 회원 주입을 준비한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @BeforeEach
    void setUp() {
        customers = mock(CustomerService.class);
        stores = mock(StoreService.class);
        member = mock(Member.class);
        store = mock(Store.class);
        customer = mock(Customer.class);
        when(member.getId()).thenReturn(10L);
        when(member.getEmail()).thenReturn("owner@test.invalid");
        when(store.getId()).thenReturn(900101L);
        when(customer.getId()).thenReturn(100L);
        when(customer.getName()).thenReturn("김단골");
        when(customer.getPhone()).thenReturn("010-1234-5678");
        when(customer.getBirthMonth()).thenReturn(5);
        when(customer.getMemo()).thenReturn("기존 메모");
        when(customer.isAgreeMarketing()).thenReturn(true);
        when(stores.getMyStores("owner@test.invalid")).thenReturn(List.of(store));
        when(stores.findOwnedStore(10L, 900101L)).thenReturn(store);
        when(customers.findCustomer(900101L, 100L)).thenReturn(customer);
        when(customers.findCustomerPage(eq(900101L), nullable(String.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(customer)));

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver views = new InternalResourceViewResolver();
        views.setPrefix("/templates/");
        views.setSuffix(".html");

        mvc = standaloneSetup(new CustomerViewController(customers, stores))
                .setValidator(validator)
                .setViewResolvers(views)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    /** 로그인 회원 매개변수만 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(LoginMember.class)
                                && parameter.getParameterType() == Member.class;
                    }

                    /** 실제 인증 대신 현재 테스트의 회원을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer container,
                                                  NativeWebRequest request,
                                                  WebDataBinderFactory binderFactory) {
                        return member;
                    }
                }).build();
    }

    /** 검증 도구 자원을 정리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @AfterEach
    void tearDown() {
        validator.close();
    }

    /**
     * 요청 유형별 경로와 정상 입력을 구성한다.
     * @param action 목록·등록·상세·수정 화면·수정 저장 구분
     * @return 매장 ID를 포함하는 요청
     * 작성자: 김순민 / 작성일: 2026-11-11
     */
    private MockHttpServletRequestBuilder request(String action) {
        MockHttpServletRequestBuilder builder = switch (action) {
            case "list" -> get("/customers");
            case "register" -> post("/customers");
            case "detail" -> get("/customers/100");
            case "edit" -> get("/customers/100/edit");
            case "update" -> post("/customers/100/edit");
            case "delete" -> post("/customers/100/delete");
            default -> throw new IllegalArgumentException("잘못된 테스트 요청");
        };
        return builder.param("storeId", "900101")
                .param("name", "김단골").param("phone", "01012345678")
                .param("birthMonth", "5").param("memo", "작성한 메모")
                .param("agreeMarketing", "true");
    }

    /** 비로그인은 모든 고객 화면 처리에서 로그인으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @ParameterizedTest
    @ValueSource(strings = {"list", "register", "detail", "edit", "update", "delete"})
    @DisplayName("로그인 회원이 없으면 로그인 화면으로 이동하고 Service를 호출하지 않는다")
    void anonymousRedirectsToLogin(String action) throws Exception {
        member = null;
        mvc.perform(request(action)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(customers, stores);
    }

    /** 모든 경로에서 소유하지 않은 매장은 고객 처리 전에 차단한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @ParameterizedTest
    @ValueSource(strings = {"list", "register", "detail", "edit", "update", "delete"})
    @DisplayName("다른 사람 매장은 403이며 고객 Service를 호출하지 않는다")
    void rejectsUnownedStore(String action) throws Exception {
        when(stores.findOwnedStore(10L, 900101L)).thenThrow(new IllegalArgumentException("소유 매장 아님"));
        mvc.perform(request(action)).andExpect(status().isForbidden());
        verifyNoInteractions(customers);
    }

    /** 소속이 맞지 않거나 없는 고객은 상세·수정에서 404로 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @ParameterizedTest
    @ValueSource(strings = {"detail", "edit", "update"})
    @DisplayName("해당 매장에 없는 고객은 404이며 수정하지 않는다")
    void missingCustomerReturns404(String action) throws Exception {
        when(customers.findCustomer(900101L, 100L)).thenThrow(new IllegalArgumentException("고객 없음"));
        mvc.perform(request(action)).andExpect(status().isNotFound());
        verify(customers, never()).updateCustomer(anyLong(), anyLong(), any());
    }

    /** 목록에 검색 결과·페이지·선택 매장·빈 등록 Form을 전달한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("목록 요청의 검색어·페이지를 전달하고 목록 화면을 반환한다")
    void listDisplaysPage() throws Exception {
        mvc.perform(get("/customers").param("storeId", "900101").param("keyword", "  단골  ").param("page", "0"))
                .andExpect(status().isOk()).andExpect(view().name("customer/list"))
                .andExpect(model().attribute("keyword", "단골"))
                .andExpect(model().attribute("selectedStore", store))
                .andExpect(model().attribute("customers", List.of(customer)))
                .andExpect(model().attributeExists("customerPage", "customerForm"));
        verify(customers).findCustomerPage(900101L, "  단골  ", 0);
    }

    /** 매장 미지정 시 첫 소유 매장과 검색 조건으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("매장을 생략하면 첫 소유 매장으로 이동하며 검색어·페이지를 유지한다")
    void missingStoreRedirectsToFirstStore() throws Exception {
        mvc.perform(get("/customers").param("keyword", "5678").param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers?storeId=900101&keyword=5678&page=1"));
        verifyNoInteractions(customers);
    }

    /** 소유 매장이 없으면 정상 안내 화면을 제공한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("소유 매장이 없으면 오류 대신 연결 안내를 표시한다")
    void noStoresReturnsGuidance() throws Exception {
        when(stores.getMyStores("owner@test.invalid")).thenReturn(List.of());
        mvc.perform(request("list")).andExpect(status().isOk())
                .andExpect(view().name("customer/no-store")).andExpect(model().attribute("closedOnly", false));
        verifyNoInteractions(customers);
    }

    /** 폐업만 남은 계정은 고객 업무 대신 통계를 안내한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("소유 매장이 모두 폐업이면 과거 통계 안내 화면을 표시한다")
    void closedStoresShowReadOnlyGuidance() throws Exception {
        when(store.isClosed()).thenReturn(true);
        mvc.perform(request("list")).andExpect(status().isOk())
                .andExpect(view().name("customer/no-store"))
                .andExpect(model().attribute("closedOnly", true))
                .andExpect(model().attribute("closedStoreId", 900101L));
        verifyNoInteractions(customers);
    }

    /** 범위 밖 페이지 요청은 마지막 페이지로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("마지막 페이지를 넘으면 매장·검색어를 유지하여 마지막 페이지로 이동한다")
    void excessivePageRedirectsToLastPage() throws Exception {
        when(customers.findCustomerPage(900101L, "5678", 9))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(9, 10), 12));
        mvc.perform(get("/customers").param("storeId", "900101").param("keyword", "5678").param("page", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers?storeId=900101&keyword=5678&page=1"));
    }

    /** 등록 성공 시 입력값을 전달하고 선택 매장의 목록으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("등록 성공 시 입력값을 Service에 전달하고 목록으로 이동한다")
    void registrationRedirectsToList() throws Exception {
        mvc.perform(request("register")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers?storeId=900101"));
        ArgumentCaptor<CustomerForm> captor = ArgumentCaptor.forClass(CustomerForm.class);
        verify(customers).registerCustomer(eq(store), captor.capture());
        CustomerForm form = captor.getValue();
        assertAll(() -> assertEquals("김단골", form.getName()),
                () -> assertEquals("01012345678", form.getPhone()),
                () -> assertEquals(Integer.valueOf(5), form.getBirthMonth()),
                () -> assertEquals("작성한 메모", form.getMemo()),
                () -> assertTrue(form.isAgreeMarketing()));
    }

    /** 등록·수정의 Form 오류 시 입력값을 유지하고 저장하지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @ParameterizedTest
    @ValueSource(strings = {"register", "update"})
    @DisplayName("이름 검증 오류는 입력값을 유지하여 재표시하고 저장하지 않는다")
    void validationErrorPreservesInput(String action) throws Exception {
        var builder = request(action);
        builder.with(servletRequest -> {
            servletRequest.setParameter("name", "   ");
            return servletRequest;
        });
        var result = mvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(view().name(action.equals("register") ? "customer/list" : "customer/edit"))
                .andExpect(model().attributeHasFieldErrors("customerForm", "name"))
                .andReturn();
        CustomerForm form = (CustomerForm) result.getModelAndView().getModel().get("customerForm");
        assertEquals("   ", form.getName());
        assertEquals("작성한 메모", form.getMemo());
        assertEquals("01012345678", form.getPhone());
        verify(customers, never()).registerCustomer(any(), any());
        verify(customers, never()).updateCustomer(anyLong(), anyLong(), any());
        if (action.equals("register")) {
            assertEquals(true, result.getModelAndView().getModel().get("showRegistration"));
            assertNotNull(result.getModelAndView().getModel().get("customerPage"));
        }
    }

    /** 업무상 전화번호 오류는 해당 입력칸에 표시하고 제출값을 유지한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @ParameterizedTest
    @ValueSource(strings = {"register", "update"})
    @DisplayName("전화번호 중복 오류는 phone 필드 오류로 표시한다")
    void duplicatePhoneShowsFieldError(String action) throws Exception {
        if (action.equals("register")) {
            when(customers.registerCustomer(eq(store), any())).thenThrow(new IllegalArgumentException("이미 등록된 고객입니다."));
        } else {
            doThrow(new IllegalArgumentException("이미 등록된 고객입니다."))
                    .when(customers).updateCustomer(eq(900101L), eq(100L), any());
        }
        var result = mvc.perform(request(action)).andExpect(status().isOk())
                .andExpect(view().name(action.equals("register") ? "customer/list" : "customer/edit"))
                .andExpect(model().attributeHasFieldErrors("customerForm", "phone")).andReturn();
        var errors = (org.springframework.validation.BindingResult) result.getModelAndView().getModel()
                .get(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "customerForm");
        assertEquals("이미 등록된 고객입니다.", errors.getFieldError("phone").getDefaultMessage());
    }

    /** 상세 화면에 조회한 고객과 매장을 전달한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("상세 조회 성공 시 고객과 매장을 화면에 전달한다")
    void detailDisplaysCustomer() throws Exception {
        mvc.perform(request("detail")).andExpect(status().isOk()).andExpect(view().name("customer/detail"))
                .andExpect(model().attribute("customer", customer)).andExpect(model().attribute("selectedStore", store));
    }

    /** 수정 화면의 Form에 기존 고객 필드를 모두 채운다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("수정 화면의 Form에 기존 고객 정보를 채운다")
    void editDisplaysExistingValues() throws Exception {
        var result = mvc.perform(request("edit")).andExpect(status().isOk())
                .andExpect(view().name("customer/edit")).andExpect(model().attribute("customerId", 100L)).andReturn();
        CustomerForm form = (CustomerForm) result.getModelAndView().getModel().get("customerForm");
        assertAll(() -> assertEquals("김단골", form.getName()),
                () -> assertEquals("010-1234-5678", form.getPhone()),
                () -> assertEquals(Integer.valueOf(5), form.getBirthMonth()),
                () -> assertEquals("기존 메모", form.getMemo()),
                () -> assertTrue(form.isAgreeMarketing()));
    }

    /** 수정 성공 시 대상 고객 상세로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("수정 성공 시 해당 매장의 고객 상세로 이동한다")
    void updateRedirectsToDetail() throws Exception {
        mvc.perform(request("update")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/100?storeId=900101"));
        verify(customers).updateCustomer(eq(900101L), eq(100L), any(CustomerForm.class));
    }

    /** 숫자로 변환할 수 없는 매장 ID 요청은 400으로 거부한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("매장 ID가 숫자가 아니면 400을 반환한다")
    void invalidStoreParameterReturns400() throws Exception {
        mvc.perform(get("/customers").param("storeId", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(customers, stores);
    }
}
