package com.team1.crm.auth;

import com.team1.crm.customer.*;
import com.team1.crm.loyalty.LoyaltyApiExceptionHandler;
import com.team1.crm.home.HomeController;
import com.team1.crm.admin.*;
import com.team1.crm.analytics.DashboardViewController;
import com.team1.crm.loyalty.LoyaltyViewController;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 요구사항 재점검에서 발견한 회귀를 방지한다. 김순민, 2026-11-23 */
class RequirementsFixTest {
    @Test void invalidStoreJsonReturns400BeforeCallingRegistration() throws Exception {
        var stores = mock(StoreService.class);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new StoreApiController(stores)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/stores")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "x".repeat(51) + "\",\"pointRate\":21}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        verifyNoInteractions(stores);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void productionRegistersBusinessControllers() {
        Class<?>[] controllers = {HomeController.class, CustomerViewController.class,
                AdminViewController.class, AdminOwnerController.class, DashboardViewController.class,
                LoyaltyViewController.class};
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            for (Class<?> controller : controllers) {
                for (Class<?> dependency : controller.getConstructors()[0].getParameterTypes()) {
                    if (!context.containsBeanDefinition(dependency.getName())) {
                        context.registerBean(dependency.getName(), (Class) dependency, () -> mock(dependency));
                    }
                }
                context.register(controller);
            }
            context.refresh();
            for (Class<?> controller : controllers) assertNotNull(context.getBean(controller));
        }
    }

    @Test void registrationUsesSameNormalizedAddressForCheckAndSave() {
        var stores = mock(StoreRepository.class);
        var members = mock(MemberRepository.class);
        var member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(members.findByEmail("owner@test.local")).thenReturn(Optional.of(member));
        when(stores.save(any(Store.class))).thenAnswer(call -> call.getArgument(0));
        var service = new StoreService(stores, members);
        var saved = service.registerStore("owner@test.local", " 매장 ", "카페", " 주소 ", 5);
        verify(stores).existsByOwner_IdAndNameAndAddress(1L, "매장", "주소");
        assertEquals("주소", saved.getAddress());
        when(stores.existsByOwner_IdAndNameAndAddress(1L, "매장", "주소")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.registerStore("owner@test.local", "매장", "카페", " 주소 ", 5));
    }

    @Test void storeFormRejectsOversizedValuesAndInvalidRate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var form = new StoreForm();
            form.setName("가".repeat(51)); form.setCategory("가".repeat(31));
            form.setAddress("가".repeat(201)); form.setPointRate(21);
            assertEquals(4, factory.getValidator().validate(form).size());
        }
    }

    @Test void missingCustomerMapsTo404AndBusinessValidationRemains400() {
        var service = new CustomerService(mock(CustomerRepository.class), mock(StoreRepository.class));
        var exception = assertThrows(CustomerNotFoundException.class, () -> service.findCustomer(1L, 2L));
        var handler = new LoyaltyApiExceptionHandler();
        assertEquals(404, handler.handleCustomerNotFound(exception).getStatusCode().value());
        assertEquals(400, handler.handleBadRequest(new IllegalArgumentException("잔액 부족")).getStatusCode().value());
    }
}
