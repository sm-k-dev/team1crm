package com.team1.crm.auth;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

/** 본인·소유 매장 수정과 중복·권한·화면을 검증한다. 작성자: 김순민 / 작성일: 2026-11-12 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:my-settings-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import(MySettingsService.class)
class MySettingsTest {
    @Autowired EntityManager em;
    @Autowired MySettingsService service;

    /** 본인 이름 변경은 다른 인증 정보를 변경하지 않는다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("내 이름 수정은 자기 이름을 허용하고 이메일·비밀번호·권한을 보존한다")
    void updatesOnlyOwnNickname() {
        var member = member("me"); var form = new MyProfileForm(); form.setNickname("me");
        service.updateProfile(member.getId(), form);
        form.setNickname(" 변경 이름 "); service.updateProfile(member.getId(), form); em.flush(); em.clear();
        var saved = em.find(Member.class, member.getId());
        assertEquals("변경 이름", saved.getNickname()); assertEquals("me@test.invalid", saved.getEmail());
        assertEquals("unchanged-hash", saved.getPassword()); assertEquals(Role.USER, saved.getRole());
    }

    /** 다른 계정의 이름 중복은 오류 화면으로 반환한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("내 정보 중복 오류는 입력을 유지하고 실제 화면에 표시한다")
    void profileDuplicateRendersError() {
        var member = member("first"); member("taken");
        var form = new MyProfileForm(); form.setNickname("taken");
        var errors = new BeanPropertyBindingResult(form, "profileForm"); var model = new ExtendedModelMap();
        model.addAttribute("profileForm", form); model.addAttribute("org.springframework.validation.BindingResult.profileForm", errors);
        assertEquals("auth/my-profile", new MySettingsController(service).updateProfile(member, form, errors, model, new RedirectAttributesModelMap()));
        assertTrue(errors.hasFieldErrors("nickname")); assertEquals("first", service.profile(member.getId()).nickname());
        assertTrue(render("auth/my-profile", model, member).contains("이미 사용 중인 이름입니다."));
    }

    /** 같은 사장님 안의 중복만 거절하며 소유 관계를 보존한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("내 매장 수정은 소유자 범위 이름 중복을 검사하고 타 소유자 동명 매장은 허용한다")
    void editsStoreWithScopedDuplicateCheck() {
        var owner = member("owner"); var other = member("other");
        var first = store(owner, "기존 매장"); store(owner, "내 다른 매장"); store(other, "타 사장님 매장");
        var form = form("내 다른 매장");
        assertThrows(IllegalArgumentException.class, () -> service.updateStore(owner.getId(), first.getId(), form));
        form.setName("타 사장님 매장"); service.updateStore(owner.getId(), first.getId(), form); em.flush(); em.clear();
        var saved = em.find(Store.class, first.getId());
        assertEquals("타 사장님 매장", saved.getName()); assertEquals(owner.getId(), saved.getOwner().getId());
        assertEquals(10, saved.getPointRate());
        service.updateStore(owner.getId(), first.getId(), form);
    }

    /** 다른 사장님·폐업 매장 변경과 미로그인 요청을 처리한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @Test @DisplayName("내 매장 수정은 타 소유자403·폐업409를 적용하고 기존 폼을 렌더링한다")
    void protectsStoreAndRendersForm() {
        var owner = member("allowed"); var other = member("blocked"); var store = store(owner, "화면 검증");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.updateStore(other.getId(), store.getId(), form("침입"))).getStatusCode().value());
        var controller = new MySettingsController(service); var model = new ExtendedModelMap();
        assertEquals("auth/my-store-edit", controller.editStore(owner, store.getId(), model));
        assertTrue(render("auth/my-store-edit", model, owner).contains("화면 검증"));
        assertEquals("redirect:/login", controller.profile(null, new ExtendedModelMap()));
        store.close(java.time.LocalDateTime.now()); em.flush();
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.updateStore(owner.getId(), store.getId(), form("폐업 수정"))).getStatusCode().value());
    }

    /** 테스트 계정을 저장한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private Member member(String name) { var member = new Member(name + "@test.invalid", "unchanged-hash", name, Role.USER); em.persist(member); return member; }
    /** 테스트 매장을 저장한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private Store store(Member owner, String name) { var store = new Store(owner, name, "카페", "서울", 5); em.persist(store); return store; }
    /** 정상 수정값을 준비한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private MyStoreForm form(String name) { var form = new MyStoreForm(); form.setName(name); form.setPointRate(10); return form; }

    /** 실제 폼 바인딩과 오류 표시를 확인한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private String render(String template, ExtendedModelMap model, Member member) {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var servlet = new org.springframework.mock.web.MockServletContext();
        var request = new org.springframework.mock.web.MockHttpServletRequest(servlet); request.setRequestURI("/my-profile");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var app = new org.springframework.web.context.support.StaticWebApplicationContext(); app.setServletContext(servlet); app.refresh();
        servlet.setAttribute(org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, app);
        var web = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet);
        var context = new org.thymeleaf.context.WebContext(web.buildExchange(request, response), java.util.Locale.KOREAN);
        context.setVariables(model); context.setVariable("loginMember", member);
        var rc = new org.springframework.web.servlet.support.RequestContext(request, response, servlet, model);
        context.setVariable("thymeleafRequestContext", new org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext(rc, request));
        try { return engine.process(template, context); } finally { app.close(); }
    }
}
