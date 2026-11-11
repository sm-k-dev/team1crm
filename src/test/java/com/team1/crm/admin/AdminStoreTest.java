package com.team1.crm.admin;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.Store;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 전체 매장 DB 조회·Controller 권한 분기·실제 템플릿 렌더링을 검증한다.
 * 보안 필터 전체는 실행하지 않으며 개발 MySQL은 사용하지 않는다.
 * 작성자: 김순민 / 작성일: 2026-11-11
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-store-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import(AdminStoreService.class)
class AdminStoreTest {
    @Autowired EntityManager em;
    @Autowired AdminStoreService service;

    /** 여러 소유자의 매장과 중복 없는 계정 수를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("관리자는 모든 매장을 조회하며 같은 사장님 계정은 한 번만 센다")
    void listsAllStoresAndDistinctOwners() {
        Member first = new Member("first@test.invalid", "unused", "첫 사장님", Role.USER);
        Member second = new Member("second@test.invalid", "unused", "둘째 사장님", Role.USER);
        em.persist(first);
        em.persist(second);
        em.persist(new Store(first, "첫 매장 <테스트>", "카페", "서울", 5));
        em.persist(new Store(first, "두 번째 매장", null, null, 5));
        em.persist(new Store(second, "다른 사장님 매장", "식당", "부산", 5));
        em.flush();
        em.clear();
        var model = new ExtendedModelMap();
        assertEquals("admin/stores", controller().stores(admin(), model));
        assertEquals(3, model.get("storeCount"));
        assertEquals(3L, model.get("linkedStoreCount"));
        assertEquals(2L, model.get("linkedOwnerCount"));
        var rows = service.findStores();
        assertEquals(first.getId(), rows.getFirst().ownerId());
        assertEquals("first@test.invalid", rows.getFirst().ownerEmail());
        assertTrue(rows.getFirst().id() < rows.getLast().id());
        String html = render(model);
        assertTrue(html.contains("첫 매장 &lt;테스트&gt;"));
        assertTrue(html.contains("다른 사장님 매장"));
        assertFalse(html.contains("등록된 매장이 없습니다."));
    }

    /** 매장이 없는 경우에도 정상 화면을 제공한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("전체 매장이 없으면 0건과 빈 목록 안내를 표시한다")
    void rendersEmptyList() {
        var model = new ExtendedModelMap();
        controller().stores(admin(), model);
        assertEquals(0, model.get("storeCount"));
        assertEquals(0L, model.get("linkedOwnerCount"));
        assertTrue(render(model).contains("등록된 매장이 없습니다."));
    }

    /** 비관리자에게 전체 목록을 노출하지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님은 전체 매장 조회 시 403으로 차단한다")
    void rejectsOwner() {
        var owner = new Member("owner@test.invalid", "unused", "사장님", Role.USER);
        var error = assertThrows(ResponseStatusException.class,
                () -> controller().stores(owner, new ExtendedModelMap()));
        assertEquals(403, error.getStatusCode().value());
    }

    /** 미로그인 요청은 로그인으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("미로그인 사용자는 로그인 화면으로 이동한다")
    void redirectsAnonymous() {
        assertEquals("redirect:/login", controller().stores(null, new ExtendedModelMap()));
    }

    /** 같은 사장님에게 두 매장을 등록할 수 있다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("관리자는 기존 사장님에게 여러 매장을 등록할 수 있다")
    void createsMultipleStoresForOwner() {
        var owner = new Member("multi@test.invalid", "unused", "사장님", Role.USER);
        em.persist(owner);
        var form = form(owner.getId());
        service.create(form);
        service.create(form);
        em.flush(); em.clear();
        assertEquals(2, service.findStores().size());
        assertTrue(service.findStores().stream().allMatch(row -> row.ownerId().equals(owner.getId())));
    }

    /** 변경 감지로 매장 정보와 소유자 변경을 저장한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("매장 수정은 정보·적립률·소유자 변경을 DB에 반영한다")
    void updatesStoreAndOwner() {
        var first = new Member("before@test.invalid", "unused", "이전", Role.USER);
        var next = new Member("after@test.invalid", "unused", "변경", Role.USER);
        em.persist(first); em.persist(next);
        Long id = service.create(form(first.getId()));
        var form = form(next.getId());
        form.setName(" 수정 매장 "); form.setPointRate(10);
        service.update(id, form);
        em.flush(); em.clear();
        var saved = service.findForm(id);
        assertEquals("수정 매장", saved.getName());
        assertEquals(next.getId(), saved.getOwnerId());
        assertEquals(10, saved.getPointRate());
    }

    /** 잘못된 소유자는 저장 전에 거절한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("관리자·없는 회원은 매장 소유자로 지정할 수 없다")
    void rejectsInvalidOwners() {
        var administrator = admin(); em.persist(administrator);
        assertThrows(IllegalArgumentException.class, () -> service.create(form(administrator.getId())));
        assertThrows(IllegalArgumentException.class, () -> service.create(form(Long.MAX_VALUE)));
        assertEquals(0, service.findStores().size());
    }

    /** 검증 오류 시 수정 폼과 기존 DB 값을 유지한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("수정 오류는 입력값과 필드 오류를 유지하고 DB를 변경하지 않는다")
    void retainsErrorsWithoutSaving() {
        var owner = new Member("validation@test.invalid", "unused", "사장님", Role.USER);
        em.persist(owner);
        Long id = service.create(form(owner.getId()));
        var invalid = form(owner.getId()); invalid.setName(" "); invalid.setPointRate(101);
        var errors = new BeanPropertyBindingResult(invalid, "storeForm");
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet(); validator.validate(invalid, errors);
        }
        var model = new ExtendedModelMap();
        model.addAttribute("storeForm", invalid);
        model.addAttribute("org.springframework.validation.BindingResult.storeForm", errors);
        assertEquals("admin/stores", controller().update(admin(), id, invalid, errors,
                model, new RedirectAttributesModelMap()));
        assertEquals(id, model.get("editingId"));
        assertTrue(errors.hasFieldErrors("name"));
        assertTrue(errors.hasFieldErrors("pointRate"));
        assertEquals("테스트 매장", service.findForm(id).getName());
        assertTrue(render(model).contains("매장명을 입력해 주세요."));
    }

    /** 등록 요청의 성공 이동과 POST 권한 검사를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("등록 성공은 목록으로 이동하고 사장님 등록 요청은 차단한다")
    void createRedirectsAndRejectsOwner() {
        var owner = new Member("post@test.invalid", "unused", "사장님", Role.USER); em.persist(owner);
        var form = form(owner.getId());
        assertThrows(ResponseStatusException.class, () -> controller().create(owner, form,
                new BeanPropertyBindingResult(form, "storeForm"), new ExtendedModelMap(), new RedirectAttributesModelMap()));
        assertEquals("redirect:/admin/stores", controller().create(admin(), form,
                new BeanPropertyBindingResult(form, "storeForm"), new ExtendedModelMap(), new RedirectAttributesModelMap()));
        assertEquals(1, service.findStores().size());
    }

    /** 없는 매장은 생성되지 않고 404로 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("존재하지 않는 매장 수정 화면은 404를 반환한다")
    void missingStoreReturns404() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller().edit(admin(), Long.MAX_VALUE, new ExtendedModelMap())).getStatusCode().value());
    }

    /** 기본 정상 입력을 생성한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private AdminStoreForm form(Long ownerId) {
        var form = new AdminStoreForm();
        form.setName("테스트 매장"); form.setCategory("카페"); form.setAddress("서울"); form.setOwnerId(ownerId);
        return form;
    }

    /** 실제 조회 서비스를 연결한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private AdminViewController controller() { return new AdminViewController(service); }

    /** 관리자 테스트 계정을 만든다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private Member admin() { return new Member("admin@test.invalid", "unused", "관리자", Role.ADMIN); }

    /** 실제 화면 템플릿의 필드 연결과 출력 처리를 검증한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private String render(ExtendedModelMap model) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet);
        request.setRequestURI("/admin/stores");
        var response = new MockHttpServletResponse();
        var appContext = new org.springframework.web.context.support.StaticWebApplicationContext();
        appContext.setServletContext(servlet);
        appContext.refresh();
        servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appContext);
        var web = JakartaServletWebApplication.buildApplication(servlet);
        var context = new WebContext(web.buildExchange(request, response), Locale.KOREAN);
        context.setVariables(model);
        context.setVariable("loginMember", admin());
        var requestContext = new org.springframework.web.servlet.support.RequestContext(request, response, servlet, model);
        context.setVariable("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(requestContext, request));
        try { return engine.process("admin/stores", context); }
        finally { appContext.close(); }
    }
}
