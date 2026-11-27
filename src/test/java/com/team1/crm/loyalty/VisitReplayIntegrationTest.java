package com.team1.crm.loyalty;

import com.team1.crm.auth.*;
import com.team1.crm.auth.security.LoginMember;
import com.team1.crm.customer.*;
import com.team1.crm.loyalty.coupon.*;
import com.team1.crm.loyalty.point.*;
import com.team1.crm.loyalty.visit.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * T29: 같은 HTTP 입력을 별도 트랜잭션으로 재전송해 DB 저장 결과를 확인한다.
 * 인증 주체만 테스트에서 제공하며 실제 Controller·Service·H2를 사용한다.
 * 같은 키 재시도는 최초 결과를 반환하고 내용 변경·동시 요청도 검증한다.
 * 작성자 김순민, 2026-11-27.
 */
@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:visit-replay;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never"})
@ActiveProfiles("test")
@Import({CustomerService.class,StoreService.class,VisitService.class,PointService.class,CouponService.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class VisitReplayIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @Autowired VisitService visits;
    @Autowired StoreService stores;
    @Autowired CouponService coupons;
    Member owner; Store store; Customer customer; MockMvc mvc;
    String requestKey;

    @BeforeEach void setup() {
        requestKey=java.util.UUID.randomUUID().toString();
        // 오직 이 클래스 전용 메모리 DB의 테스트 데이터를 초기화한다.
        for(String table:new String[]{"point_history","visit","coupon","customer","store","password_reset_tokens","member"})
            jdbc.update("delete from "+table);
        new TransactionTemplate(manager).executeWithoutResult(status->{
            owner=new Member("replay@test.invalid","unused","재전송검증",Role.USER);em.persist(owner);
            store=new Store(owner,"재전송매장","카페",null,5);em.persist(store);
            customer=new Customer(store,"검증고객","010-9000-0001",null,null,false,null);em.persist(customer);em.flush();
        });
        mvc=standaloneSetup(new VisitApiController(visits,stores))
                .setControllerAdvice(new LoyaltyApiExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver(){
                    public boolean supportsParameter(MethodParameter p){return p.hasParameterAnnotation(LoginMember.class);}
                    public Object resolveArgument(MethodParameter p,ModelAndViewContainer m,NativeWebRequest r,WebDataBinderFactory b){return owner;}
                }).build();
    }
    String path(){return "/api/customers/"+customer.getId()+"/visits";}
    String body(Long coupon,int used){return "{\"requestKey\":\""+requestKey+"\",\"amount\":10000,\"visitedAt\":\"2026-11-27T12:00:00\",\"menuMemo\":\"same request\",\"usedPoints\":"+used+
            (coupon==null?"":",\"couponId\":"+coupon)+"}";}
    String request(String json,int expected) throws Exception {
        return mvc.perform(post(path()).param("storeId",store.getId().toString()).contentType("application/json").content(json))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }
    long count(String table){return jdbc.queryForObject("select count(*) from "+table+" where customer_id=?",Long.class,customer.getId());}

    @Test void identicalRequestReturnsOriginalResultAndWritesOnlyOnce() throws Exception {
        String json=body(null,0);String first=request(json,201);assertEquals(first,request(json,201));
        assertEquals(1,count("visit"));assertEquals(1,count("point_history"));assertEquals(1,count("visit_request"));
        assertEquals(10000,jdbc.queryForObject("select sum(amount) from visit where customer_id=?",Long.class,customer.getId()));
        assertEquals(500,jdbc.queryForObject("select sum(point) from point_history where customer_id=?",Long.class,customer.getId()));
    }
    @Test void sameKeyCouponReplaySucceedsButNewRequestCannotReuseCoupon() throws Exception {
        CouponForm f=new CouponForm();f.setTitle("재전송쿠폰");f.setDiscount(1000);f.setExpiresAt(LocalDate.now().plusDays(1));
        long coupon=coupons.issue(store.getId(),customer.getId(),f);
        String json=body(coupon,0);String first=request(json,201);assertEquals(first,request(json,201));
        requestKey=java.util.UUID.randomUUID().toString();request(body(coupon,0),400);
        assertEquals(1,count("visit"));assertEquals(1,count("point_history"));
        assertEquals(9000,jdbc.queryForObject("select sum(amount) from visit where customer_id=?",Long.class,customer.getId()));
        assertEquals(1,jdbc.queryForObject("select count(*) from coupon where id=? and used_at is not null",Long.class,coupon));
    }
    @Test void insufficientPointReplayRollsBackCouponUseAndAllTransactionWrites() throws Exception {
        CouponForm f=new CouponForm();f.setTitle("롤백쿠폰");f.setDiscount(1000);f.setExpiresAt(LocalDate.now().plusDays(1));
        long coupon=coupons.issue(store.getId(),customer.getId(),f);
        String json=body(coupon,100);request(json,400);request(json,400);
        assertEquals(0,count("visit"));assertEquals(0,count("point_history"));
        assertEquals(1,jdbc.queryForObject("select count(*) from coupon where id=? and used_at is null",Long.class,coupon));
        assertEquals(0,count("visit_request"));
        // 실패한 요청은 키를 소비하지 않는다. 같은 키로 올바른 입력을 다시 보낼 수 있다.
        request(body(coupon,0),201);assertEquals(1,count("visit_request"));
    }

    @Test void newKeyAllowsAnotherLegitimateVisitWithSameValues() throws Exception {
        request(body(null,0),201);requestKey=java.util.UUID.randomUUID().toString();request(body(null,0),201);
        assertEquals(2,count("visit"));assertEquals(2,count("visit_request"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"amount","visitedAt","menuMemo","usedPoints","couponId"})
    void sameKeyWithChangedPayloadReturns409(String field) throws Exception {
        String json=body(null,0);request(json,201);
        String changed=switch(field){
            case "amount" -> json.replace("10000","20000");
            case "visitedAt" -> json.replace("12:00:00","12:01:00");
            case "menuMemo" -> json.replace("same request","changed memo");
            case "usedPoints" -> json.replace("\"usedPoints\":0","\"usedPoints\":1");
            default -> json.replace("}",",\"couponId\":999}");
        };
        request(changed,409);assertEquals(1,count("visit"));assertEquals(1,count("point_history"));
    }

    @Test void blankOrMalformedKeyIsRejectedBeforeSaving() throws Exception {
        String json=body(null,0);
        request(json.replace(requestKey,""),400);request(json.replace(requestKey,"bad-key"),400);
        request(json.replace("\"requestKey\":\""+requestKey+"\",",""),400);
        assertEquals(0,count("visit"));
    }

    @Test void missingVisitTimeIsStableAcrossRetriesAndRateChanges() throws Exception {
        String json=body(null,0).replace("\"visitedAt\":\"2026-11-27T12:00:00\",","");
        String first=request(json,201);
        jdbc.update("update store set point_rate=20 where id=?",store.getId());
        assertEquals(first,request(json,201));assertEquals(1,count("visit"));
        assertEquals(500,jdbc.queryForObject("select sum(point) from point_history where customer_id=?",Long.class,customer.getId()));
    }

    @Test void concurrentIdenticalRequestsCommitOneVisitAndOneEarn() throws Exception {
        String json=body(null,0);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<String> task=()->{ready.countDown();assertTrue(start.await(10,java.util.concurrent.TimeUnit.SECONDS));return request(json,201);};
            var first=pool.submit(task);var second=pool.submit(task);
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));start.countDown();
            assertEquals(first.get(15,java.util.concurrent.TimeUnit.SECONDS),second.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,count("visit"));assertEquals(1,count("point_history"));assertEquals(1,count("visit_request"));
        } finally {start.countDown();pool.shutdownNow();}
    }
}
