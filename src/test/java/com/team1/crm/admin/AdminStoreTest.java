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
@Import({AdminStoreService.class, AdminOwnerService.class, com.team1.crm.auth.security.PasswordEncoderConfig.class})
class AdminStoreTest {
    @Autowired EntityManager em;
    @Autowired AdminStoreService service;
    @Autowired AdminOwnerService ownerService;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    /** 새 계정은 USER로 저장하고 기존 인증 인코더와 호환되어야 한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("관리자 등록 계정은 USER이고 비밀번호가 Argon2로 해시된다")
    void createsOwnerWithEncodedPassword() {
        var form = ownerForm();
        Long id = ownerService.create(form);
        em.flush(); em.clear();
        Member member = em.find(Member.class, id);
        assertEquals(Role.USER, member.getRole());
        assertEquals("new-owner@test.invalid", member.getEmail());
        assertNotEquals(form.getPassword(), member.getPassword());
        assertTrue(member.getPassword().startsWith("$argon2"));
        assertTrue(encoder.matches(form.getPassword(), member.getPassword()));
        assertTrue(service.findOwnerRows().getFirst().stores().isEmpty());
    }

    /** 기존 관리자 이메일과 사장님 이름도 중복 검사한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("기존 계정과 이메일·이름이 중복되면 해당 필드를 거절한다")
    void checksOwnerDuplicates() {
        em.persist(new Member("new-owner@test.invalid", "unused", "다른 이름", Role.ADMIN));
        assertEquals("email", ownerService.duplicateField(ownerForm()));
        var form = ownerForm(); form.setEmail("other@test.invalid"); form.setNickname("다른 이름");
        assertEquals("nickname", ownerService.duplicateField(form));
        assertEquals("email", assertThrows(IllegalArgumentException.class, () -> ownerService.create(ownerForm())).getMessage());
    }

    /** 중복 오류 화면에서 일반 입력은 유지하고 비밀번호는 제외한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("중복 이메일 오류는 목록을 다시 보여주며 비밀번호를 재출력하지 않는다")
    void duplicateOwnerReturnsFormWithoutPassword() {
        em.persist(new Member("new-owner@test.invalid", "unused", "기존 이름", Role.USER));
        var form = ownerForm();
        var model = new ExtendedModelMap(); model.addAttribute("ownerForm", form);
        var errors = new BeanPropertyBindingResult(form, "ownerForm");
        model.addAttribute("org.springframework.validation.BindingResult.ownerForm", errors);
        assertEquals("admin/owners", new AdminOwnerController(ownerService, service).create(admin(), form, errors,
                model, new RedirectAttributesModelMap()));
        assertTrue(errors.hasFieldErrors("email"));
        assertEquals("new-owner@test.invalid", form.getEmail());
        assertNull(form.getPassword());
        String html = render(model, "admin/owners");
        assertTrue(html.contains("이미 가입된 이메일입니다."));
        assertFalse(html.contains("Owner-test-123!"));
    }

    /** 권한 확인 후 정상 등록은 PRG로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님 등록 요청은 미로그인 이동·비관리자 403·관리자 성공을 구분한다")
    void protectsOwnerCreation() {
        var controller = new AdminOwnerController(ownerService, service);
        var form = ownerForm(); var errors = new BeanPropertyBindingResult(form, "ownerForm");
        assertEquals("redirect:/login", controller.create(null, form, errors, new ExtendedModelMap(), new RedirectAttributesModelMap()));
        var user = new Member("user@test.invalid", "unused", "사장님", Role.USER);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.create(user, form, errors,
                new ExtendedModelMap(), new RedirectAttributesModelMap())).getStatusCode().value());
        assertEquals("redirect:/admin/owners", controller.create(admin(), form, errors,
                new ExtendedModelMap(), new RedirectAttributesModelMap()));
    }

    /** 필수값·형식·최소 비밀번호 길이를 검사한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("이메일 형식·공백 이름·짧은 비밀번호 오류는 저장 없이 표시한다")
    void rejectsInvalidOwnerForm() {
        var form = ownerForm(); form.setEmail("invalid"); form.setNickname(" "); form.setPassword("123");
        var errors = new BeanPropertyBindingResult(form, "ownerForm");
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet(); validator.validate(form, errors);
        }
        assertTrue(errors.hasFieldErrors("email"));
        assertTrue(errors.hasFieldErrors("nickname"));
        assertTrue(errors.hasFieldErrors("password"));
        var model = new ExtendedModelMap(); model.addAttribute("ownerForm", form);
        assertEquals("admin/owners", new AdminOwnerController(ownerService, service).create(admin(), form, errors,
                model, new RedirectAttributesModelMap()));
        assertTrue(service.findOwnerRows().isEmpty());
    }

    /** 이름 수정이 인증 정보와 소유 관계를 보존하는지 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님 이름 수정은 DB에 반영되고 이메일·비밀번호·역할·매장을 보존한다")
    void updatesOwnerNameOnly() {
        Long id = ownerService.create(ownerForm());
        Member before = em.find(Member.class, id);
        String password = before.getPassword();
        em.persist(new Store(before, "이름 수정 보존 매장", "카페", "서울", 5));
        var form = new AdminOwnerEditForm(); form.setNickname(" 변경된 사장님 ");
        ownerService.updateNickname(id, form);
        em.flush(); em.clear();
        Member after = em.find(Member.class, id);
        assertEquals("변경된 사장님", after.getNickname());
        assertEquals("new-owner@test.invalid", after.getEmail());
        assertEquals(password, after.getPassword());
        assertEquals(Role.USER, after.getRole());
        assertEquals(id, service.findStores().getFirst().ownerId());
        form.setNickname(after.getNickname());
        assertDoesNotThrow(() -> ownerService.updateNickname(id, form));
    }

    /** 중복 수정 오류가 기존 저장값을 바꾸지 않는지 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("이름 수정 중복 오류는 입력값과 오류를 표시하고 기존 이름을 보존한다")
    void duplicateOwnerEditKeepsOriginal() {
        Long id = ownerService.create(ownerForm());
        em.persist(new Member("other-name@test.invalid", "unused", "사용 중 이름", Role.USER)); em.flush();
        var form = new AdminOwnerEditForm(); form.setNickname("사용 중 이름");
        var model = new ExtendedModelMap(); model.addAttribute("ownerEditForm", form);
        var errors = new BeanPropertyBindingResult(form, "ownerEditForm");
        model.addAttribute("org.springframework.validation.BindingResult.ownerEditForm", errors);
        assertEquals("admin/owner-edit", new AdminOwnerController(ownerService, service).update(admin(), id, form,
                errors, model, new RedirectAttributesModelMap()));
        assertTrue(errors.hasFieldErrors("nickname"));
        assertEquals("신규 사장님", ownerService.findOwner(id).nickname());
        assertTrue(render(model, "admin/owner-edit").contains("이미 사용 중인 이름입니다."));
    }

    /** 조회 및 저장의 대상·권한 검사를 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("이름 수정은 없는 계정·관리자 대상 404와 비관리자 요청 403을 적용한다")
    void protectsOwnerEdit() {
        var administrator = admin(); em.persist(administrator);
        var controller = new AdminOwnerController(ownerService, service);
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.edit(admin(), Long.MAX_VALUE, new ExtendedModelMap())).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.edit(admin(), administrator.getId(), new ExtendedModelMap())).getStatusCode().value());
        Long id = ownerService.create(ownerForm());
        var form = new AdminOwnerEditForm(); form.setNickname("변경 이름");
        var owner = em.find(Member.class, id);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.update(owner, id, form,
                new BeanPropertyBindingResult(form, "ownerEditForm"), new ExtendedModelMap(),
                new RedirectAttributesModelMap())).getStatusCode().value());
        assertEquals("redirect:/login", controller.edit(null, id, new ExtendedModelMap()));
        var model = new ExtendedModelMap();
        assertEquals("admin/owner-edit", controller.edit(admin(), id, model));
        assertTrue(render(model, "admin/owner-edit").contains("신규 사장님"));
        assertEquals("redirect:/admin/owners", controller.update(admin(), id, form,
                new BeanPropertyBindingResult(form, "ownerEditForm"), new ExtendedModelMap(), new RedirectAttributesModelMap()));
    }

    /** 공백 이름은 기존 데이터를 바꾸지 않는다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("이름 수정의 공백·길이 초과는 검증 오류로 처리한다")
    void validatesOwnerEdit() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            for (String name : java.util.List.of(" ", "가".repeat(256))) {
                var form = new AdminOwnerEditForm(); form.setNickname(name);
                assertFalse(validator.validate(form).isEmpty());
            }
        }
    }

    /** 정상 계정 등록 입력을 준비한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private AdminOwnerForm ownerForm() {
        var form = new AdminOwnerForm();
        form.setEmail("new-owner@test.invalid"); form.setNickname("신규 사장님"); form.setPassword("Owner-test-123!");
        return form;
    }

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

    /** 미연결 계정과 다중 매장을 포함한 조회·출력을 검증한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님 목록은 관리자 제외·매장 없는 계정 포함·다중 매장을 표시한다")
    void listsOwnersWithAndWithoutStores() {
        var first = new Member("linked@test.invalid", "secret-not-for-view", "사장님 <테스트>", Role.USER);
        var second = new Member("unlinked@test.invalid", "unused", "미연결 사장님", Role.USER);
        em.persist(first); em.persist(second); em.persist(admin());
        em.persist(new Store(first, "첫 매장", "카페", "서울", 5));
        em.persist(new Store(first, "둘째 매장", "카페", "서울", 5));
        em.flush(); em.clear();
        var model = new ExtendedModelMap();
        assertEquals("admin/owners", controller().owners(admin(), model));
        assertEquals(2, model.get("ownerCount"));
        assertEquals(1L, model.get("linkedOwnerCount"));
        assertEquals(1L, model.get("unlinkedOwnerCount"));
        var owners = service.findOwnerRows();
        assertEquals(2, owners.getFirst().stores().size());
        assertTrue(owners.getLast().stores().isEmpty());
        String html = render(model, "admin/owners");
        assertTrue(html.contains("사장님 &lt;테스트&gt;"));
        assertTrue(html.contains("첫 매장"));
        assertTrue(html.contains("둘째 매장"));
        assertTrue(html.contains("연결된 매장이 없습니다."));
        assertFalse(html.contains("admin@test.invalid"));
        assertFalse(html.contains("secret-not-for-view"));
    }

    /** 등록된 사장님이 없을 때도 안내 화면을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님 계정이 없으면 집계 0과 빈 목록 안내를 표시한다")
    void rendersEmptyOwners() {
        var model = new ExtendedModelMap();
        controller().owners(admin(), model);
        assertEquals(0, model.get("ownerCount"));
        assertTrue(render(model, "admin/owners").contains("등록된 사장님 계정이 없습니다."));
    }

    /** 사장님 계정 정보의 관리자 전용 접근을 확인한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("사장님 목록은 미로그인 이동과 비관리자 403을 적용한다")
    void protectsOwnerList() {
        assertEquals("redirect:/login", controller().owners(null, new ExtendedModelMap()));
        var member = new Member("owner@test.invalid", "unused", "사장님", Role.USER);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller().owners(member, new ExtendedModelMap())).getStatusCode().value());
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
        return render(model, "admin/stores");
    }

    /** 지정한 관리자 화면을 실제 엔진으로 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private String render(ExtendedModelMap model, String template) {
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
        try { return engine.process(template, context); }
        finally { appContext.close(); }
    }
}
