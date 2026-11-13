package com.team1.crm.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 테스트 DB에서 가입 방식 간 중복·기존 데이터 보존을 검증한다. 작성자: 김순민 / 작성일: 2026-11-13 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:member-signup-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import({MemberService.class, MemberSignupTest.EncoderConfig.class})
class MemberSignupTest {
    @Autowired private MemberRepository repository;
    @Autowired private MemberService service;

    /** Google 계정의 표기 변형 재가입을 차단한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @ParameterizedTest
    @ValueSource(strings = {"google@example.com", "GOOGLE@EXAMPLE.COM", " google@example.com "})
    @DisplayName("Google 회원은 동일·대문자·앞뒤 공백 이메일로 일반 재가입할 수 없다")
    void googleSignupConflict(String email) {
        Member google = new Member("Google@Example.com", "", "기존구글", Role.USER);
        google.linkSocial("google", "subject-123");
        Long id = repository.saveAndFlush(google).getId();
        assertThrows(DuplicateMemberEmailException.class, () -> service.signup(form(email)));
        assertEquals(1, repository.count());
        Member unchanged = repository.findById(id).orElseThrow();
        assertEquals("google", unchanged.getProvider());
        assertEquals("subject-123", unchanged.getProviderId());
        assertEquals("", unchanged.getPassword());
    }

    /** 일반 계정과 과거 공백 데이터도 같은 비교 기준을 적용한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("기존 일반 계정의 앞뒤 공백 이메일도 중복 가입을 거절한다")
    void legacyLocalConflict() {
        repository.saveAndFlush(new Member(" Local@Example.com ", "original-hash", "기존일반", Role.USER));
        assertThrows(DuplicateMemberEmailException.class, () -> service.signup(form("LOCAL@example.com")));
        assertEquals(1, repository.count());
    }

    /** 신규 일반 회원의 저장 형식과 해시를 실제 DB에서 확인한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("새 이메일은 정규화하여 일반 계정으로 등록하고 비밀번호를 해시한다")
    void newLocalSignup() {
        Long id = service.signup(form(" New@Example.com "));
        Member saved = repository.findById(id).orElseThrow();
        assertEquals("new@example.com", saved.getEmail());
        assertNull(saved.getProvider());
        assertTrue(new BCryptPasswordEncoder().matches("password123!", saved.getPassword()));
    }

    /** 실제 Service와 Controller를 연결해 오류 응답과 행 수 보존을 확인한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("회원가입 API는 Google 이메일 중복 시 409와 Google 로그인 안내를 반환한다")
    void apiDuplicateResponse() throws Exception {
        Member google = new Member("google@example.com", "", "구글회원", Role.USER);
        google.linkSocial("google", "subject-123");
        repository.saveAndFlush(google);
        var mvc = MockMvcBuilders.standaloneSetup(new MemberApiController(service)).build();
        mvc.perform(post("/api/members/signup").contentType("application/json")
                        .content("{\"email\":\" GOOGLE@EXAMPLE.COM \",\"password\":\"password123!\",\"nickname\":\"새이름\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Google 로그인")));
        assertEquals(1, repository.count());
    }

    /** API 직접 호출에도 필수 입력 검증을 적용한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @Test @DisplayName("회원가입 API는 잘못된 이메일과 빈 비밀번호를 400으로 거절한다")
    void invalidRequest() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new MemberApiController(service)).build();
        mvc.perform(post("/api/members/signup").contentType("application/json")
                        .content("{\"email\":\"invalid\",\"password\":\"\",\"nickname\":\"새이름\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    /** 가입 요청값을 구성한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    private MemberSignupForm form(String email) {
        MemberSignupForm form = new MemberSignupForm();
        form.setEmail(email);
        form.setPassword("password123!");
        form.setNickname("신규회원");
        return form;
    }

    /** 테스트 환경에만 해시 인코더를 제공한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @TestConfiguration
    static class EncoderConfig {
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    }
}

