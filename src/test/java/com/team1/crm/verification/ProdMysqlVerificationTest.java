package com.team1.crm.verification;

import com.team1.crm.auth.*;
import com.team1.crm.customer.*;
import com.team1.crm.loyalty.coupon.*;
import com.team1.crm.loyalty.visit.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 미완료 검증일지의 prod 보안·고객 격리·폐업을 실제 MySQL 8에서 재검증한다.
 * 명시적으로 활성화한 로컬 폐기용 DB만 사용한다. 운영 DB로 URL을 바꾸지 않는다.
 * 작성자: 김순민 / 2026-11-26
 */
@EnabledIfEnvironmentVariable(named="CRM_MYSQL_VERIFICATION", matches="true")
@ActiveProfiles("prod")
@SpringBootTest(properties={
        "server.port=0",
        "spring.datasource.url=jdbc:mysql://127.0.0.1:33316/crm_validation?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
        "spring.datasource.username=root", "spring.datasource.password=crm-validation-only",
        "spring.jpa.hibernate.ddl-auto=create", "spring.sql.init.mode=never",
        "spring.security.oauth2.client.registration.google.client-id=verification-only",
        "spring.security.oauth2.client.registration.google.client-secret=verification-only",
        "jwt.secret=verification-only-32-byte-secret-for-local-tests",
        "spring.mail.username=verification@test.invalid", "spring.mail.password=verification-only",
        "app.base-url=http://localhost:18082", "app.customer.privacy.enabled=false"
})
class ProdMysqlVerificationTest {
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    @Autowired StoreService stores;
    @Autowired CustomerService customers;
    @Autowired StoreClosureService closure;
    @Autowired VisitService visits;
    @Autowired CouponService coupons;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    static final String PASSWORD="Verification2026!";

    @BeforeEach void setup() {
        mvc=webAppContextSetup(context).apply(springSecurity()).build();
        assertEquals("crm_validation",jdbc.queryForObject("SELECT DATABASE()",String.class));
        assertTrue(jdbc.queryForObject("SELECT VERSION()",String.class).startsWith("8."));
    }
    Member owner(Role role) {
        String key=UUID.randomUUID().toString();
        return members.saveAndFlush(new Member(key+"@test.invalid",passwords.encode(PASSWORD),"검증-"+key.substring(0,8),role));
    }
    Store store(Member owner,String name) {return stores.registerStore(owner.getEmail(),name,"카페","검증 주소",5);}
    CustomerForm form(String phone) {var f=new CustomerForm();f.setName("검증고객");f.setPhone(phone);f.setMemo("개인 메모");return f;}
    MockHttpSession login(Member m) throws Exception {
        return (MockHttpSession)mvc.perform(formLogin().user(m.getEmail()).password(PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }

    @Test @DisplayName("MySQL 실필터 방문 API 재전송은 같은 결과를 반환하고 내용 변경은 409")
    void visitReplayWithRealSecurityAndMysql() throws Exception {
        var owner=owner(Role.USER);var store=store(owner,"재전송HTTP");
        long customerId=customers.registerCustomer(store,form("010-9898-0001"));var session=login(owner);
        String key=UUID.randomUUID().toString();
        String body="{\"requestKey\":\""+key+"\",\"amount\":10000,\"usedPoints\":0}";
        String url="/api/customers/"+customerId+"/visits";
        String first=mvc.perform(post(url).param("storeId",store.getId().toString()).session(session).with(csrf())
                .contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(post(url).param("storeId",store.getId().toString()).session(session).with(csrf())
                .contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(content().string(first));
        mvc.perform(post(url).param("storeId",store.getId().toString()).session(session).with(csrf())
                .contentType("application/json").content(body.replace("10000","20000"))).andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("select count(*) from visit where customer_id=?",Long.class,customerId));
        assertEquals(1,jdbc.queryForObject("select count(*) from visit_request where customer_id=?",Long.class,customerId));
        assertEquals(500,jdbc.queryForObject("select sum(point) from point_history where customer_id=?",Long.class,customerId));
    }

    @Test @DisplayName("MySQL 별도 트랜잭션의 동시 재전송도 방문·적립을 한 번만 저장한다")
    void concurrentVisitReplayOnMysql() throws Exception {
        var store=store(owner(Role.USER),"동시재전송");
        long customerId=customers.registerCustomer(store,form("010-9898-0002"));
        String key=UUID.randomUUID().toString();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<VisitResult> call=()->{assertTrue(start.await(10,TimeUnit.SECONDS));var f=new VisitForm();
                f.setRequestKey(key);f.setAmount(10000);return visits.register(store.getId(),customerId,f);};
            var first=pool.submit(call);var second=pool.submit(call);start.countDown();
            assertEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("select count(*) from visit where customer_id=?",Long.class,customerId));
            assertEquals(1,jdbc.queryForObject("select count(*) from point_history where customer_id=?",Long.class,customerId));
        } finally {start.countDown();pool.shutdownNow();}
    }

    @Test @DisplayName("prod 실제 로그인·ADMIN 권한·CSRF를 검증한다")
    void productionRoutesUseRealSecurity() throws Exception {
        var owner=owner(Role.USER);var admin=owner(Role.ADMIN);var session=login(owner);
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/admin").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").session(login(admin))).andExpect(status().isOk());
        mvc.perform(get("/store-settings").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"storeForm\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/api/stores").session(session).contentType("application/json")
                .content("{\"name\":\"토큰 없음\"}")).andExpect(status().isForbidden());
    }

    @Test @DisplayName("이동한 매장 등록 API의 저장·중복·빈값 처리를 실제 필터와 DB로 확인한다")
    void registrationWithCsrfStoresAndRefreshesList() throws Exception {
        var owner=owner(Role.USER);var session=login(owner);
        String body="{\"name\":\"등록 검증\",\"category\":\"카페\",\"address\":\"테스트 주소\",\"pointRate\":5}";
        mvc.perform(post("/api/stores").session(session).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(get("/store-settings").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("등록 검증")));
        mvc.perform(post("/api/stores").session(session).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/stores").session(session).with(csrf()).contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(1,stores.getMyStores(owner.getEmail()).size());
    }

    @Test @DisplayName("MySQL에서 정규화·동일 매장 중복 거부·타 매장 허용·목록 마스킹을 검증한다")
    void normalizationAndMaskingOnMysql() throws Exception {
        var owner=owner(Role.USER);var first=store(owner,"첫 매장");var second=store(owner,"다른 매장");
        var id=customers.registerCustomer(first,form("01087654321"));
        assertEquals("010-8765-4321",customers.findCustomer(first.getId(),id).getPhone());
        assertThrows(IllegalArgumentException.class,()->customers.registerCustomer(first,form("010-8765-4321")));
        assertNotNull(customers.registerCustomer(second,form("010-8765-4321")));
        String html=mvc.perform(get("/customers").param("storeId",first.getId().toString()).session(login(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("010-****-4321"));assertFalse(html.contains("010-8765-4321"));
        for(String field:new String[]{"name","phone","birthMonth","memo"})assertTrue(html.contains("for=\""+field+"\""));
    }

    @Test @DisplayName("실제 로그인 후 타 매장 고객 조회·수정과 다른 소유자의 매장 요청을 차단한다")
    void tenantIsolationWithRealSecurityAndMysql() throws Exception {
        var owner=owner(Role.USER);var other=owner(Role.USER);
        var first=store(owner,"소유 매장");var second=store(owner,"소유한 다른 매장");var foreign=store(other,"타인 매장");
        var id=customers.registerCustomer(first,form("01011112222"));var session=login(owner);
        mvc.perform(get("/customers/"+id).param("storeId",first.getId().toString()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/customers/"+id).param("storeId",second.getId().toString()).session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/customers/"+id+"/edit").param("storeId",second.getId().toString()).param("name","변조")
                .param("phone","01033334444").session(session).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/customers").param("storeId",foreign.getId().toString()).session(session)).andExpect(status().isForbidden());
        assertEquals("검증고객",customers.findCustomer(first.getId(),id).getName());
    }

    @Test @DisplayName("MySQL 폐업 처리에서 거래·포인트·쿠폰을 보존하고 자유입력 제거 및 신규 거래 차단을 확인한다")
    void closurePreservesHistoryAndRejectsNewTransactions() {
        var owner=owner(Role.USER);var store=store(owner,"폐업 검증");var id=customers.registerCustomer(store,form("01022223333"));
        var visit=new VisitForm();visit.setRequestKey(java.util.UUID.randomUUID().toString());visit.setAmount(30000);visit.setMenuMemo("민감 방문 메모");
        visits.register(store.getId(),id,visit);
        var coupon=new CouponForm();coupon.setTitle("민감 쿠폰 제목");coupon.setDiscount(1000);coupon.setExpiresAt(LocalDate.now().plusDays(10));
        coupons.issue(store.getId(),id,coupon);
        closure.close(owner.getId(),store.getId());
        assertEquals(1,jdbc.queryForObject("select count(*) from visit where customer_id=?",Integer.class,id));
        assertEquals(30000,jdbc.queryForObject("select sum(amount) from visit where customer_id=?",Integer.class,id));
        assertEquals(1500,jdbc.queryForObject("select sum(point) from point_history where customer_id=?",Integer.class,id));
        assertEquals(1,jdbc.queryForObject("select count(*) from coupon where customer_id=?",Integer.class,id));
        assertNull(jdbc.queryForObject("select menu_memo from visit where customer_id=?",String.class,id));
        assertEquals("폐업 매장 쿠폰",jdbc.queryForObject("select title from coupon where customer_id=?",String.class,id));
        assertNull(jdbc.queryForObject("select phone from customer where id=?",String.class,id));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->visits.register(store.getId(),id,visit)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->coupons.issue(store.getId(),id,coupon)).getStatusCode().value());
    }

    @Test @DisplayName("폐업과 방문 등록을 동시에 호출해 폐업 이후 거래 증가와 개인정보 잔존이 없는지 확인한다")
    void concurrentClosureAndVisitSerializeOnMysql() throws Exception {
        var owner=owner(Role.USER);var store=store(owner,"동시성 검증");var id=customers.registerCustomer(store,form("01044445555"));
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var close=pool.submit(()->{start.await();closure.close(owner.getId(),store.getId());return true;});
            var write=pool.submit(()->{start.await();var f=new VisitForm();f.setRequestKey(java.util.UUID.randomUUID().toString());f.setAmount(1000);f.setMenuMemo("동시 메모");
                try {visits.register(store.getId(),id,f);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}});
            start.countDown();assertTrue(close.get(20,TimeUnit.SECONDS));boolean registered=write.get(20,TimeUnit.SECONDS);
            assertEquals(registered?1:0,jdbc.queryForObject("select count(*) from visit where customer_id=?",Integer.class,id));
            assertEquals(0,jdbc.queryForObject("select count(*) from visit where customer_id=? and menu_memo is not null",Integer.class,id));
            var f=new VisitForm();f.setRequestKey(java.util.UUID.randomUUID().toString());f.setAmount(1000);
            assertEquals(409,assertThrows(ResponseStatusException.class,()->visits.register(store.getId(),id,f)).getStatusCode().value());
        } finally {pool.shutdownNow();}
    }

    @Test @DisplayName("기존 컬럼이 있는 MySQL에 개인정보 마이그레이션을 두 번 실행해도 중복 ADD 오류가 없다")
    void privacyMigrationCanBeRepeated() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection()) {
            var script=new org.springframework.core.io.FileSystemResource("docs/sql/customer_privacy_migration.sql");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);
        }
        assertEquals(3,jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='customer' and column_name in ('deleted_at','anonymized_at','phone')",Integer.class));
        assertEquals("YES",jdbc.queryForObject("select is_nullable from information_schema.columns where table_schema=database() and table_name='customer' and column_name='phone'",String.class));
    }
}
