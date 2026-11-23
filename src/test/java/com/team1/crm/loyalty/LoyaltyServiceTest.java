package com.team1.crm.loyalty;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerService;
import com.team1.crm.loyalty.coupon.Coupon;
import com.team1.crm.loyalty.coupon.CouponForm;
import com.team1.crm.loyalty.coupon.CouponService;
import com.team1.crm.loyalty.point.PointService;
import com.team1.crm.loyalty.point.PointType;
import com.team1.crm.loyalty.visit.VisitForm;
import com.team1.crm.loyalty.visit.VisitResult;
import com.team1.crm.loyalty.visit.VisitService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 방문·포인트·쿠폰 서비스의 업무 규칙을 테스트 전용 메모리 DB에서 검증한다.
 * 개발용 MySQL 데이터는 사용하지 않는다.
 *
 * 쿠폰 사용과 포인트 사용은 별도 경로 없이 방문 등록 한 번에 처리하므로, 대부분의 시나리오가 방문 등록을 거친다.
 *
 * 확인하는 것: 방문 적립, 쿠폰 할인, 포인트 사용(1P = 1원)과 잔액 초과 거부, 쿠폰 중복 사용 거부,
 *              다른 매장·삭제 고객 접근 거부(매장 격리), 상세 화면용 묶음 조회.
 * 확인하지 않는 것: 실제 동시 요청(여러 스레드)에서의 잠금 동작,
 *                   검증 실패 시 쿠폰 사용까지 되돌리는 롤백 (테스트 전체가 한 트랜잭션이라 중간 롤백을 볼 수 없다).
 *
 * 작성자: 이재준
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyalty-service-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import({CustomerService.class, StoreService.class, VisitService.class, PointService.class,
        CouponService.class, LoyaltyDetailService.class})
class LoyaltyServiceTest {

    @Autowired private EntityManager em;
    @Autowired private VisitService visitService;
    @Autowired private PointService pointService;
    @Autowired private CouponService couponService;
    @Autowired private LoyaltyDetailService detailService;

    // ───────────── 방문 적립 ─────────────

    @Test
    @DisplayName("방문을 등록하면 방문과 적립 내역이 함께 저장되고 잔액이 적립액과 같다")
    void registerSavesVisitAndEarnedPoints() {
        Store store = createStore("visit-ok", 5);
        Customer customer = createCustomer(store, "010-1111-0001");

        VisitResult result = visitService.register(store.getId(), customer.getId(), visitForm(10000, "아메리카노"));
        flushAndClear();

        assertEquals(500, result.earnedPoints());
        assertEquals(10000, result.paidAmount());
        assertEquals(1, visitService.getRecentVisits(store.getId(), customer.getId()).size());
        assertEquals(1, visitService.getVisitCount(store.getId(), customer.getId()));
        assertEquals(500L, pointService.getBalance(store.getId(), customer.getId()));
        var history = pointService.getRecentHistory(store.getId(), customer.getId());
        assertEquals(1, history.size());
        assertEquals(PointType.EARN, history.get(0).getType());
    }

    @Test
    @DisplayName("적립액이 0이면 적립 내역을 만들지 않는다")
    void zeroEarnedPointsCreatesNoHistory() {
        Store store = createStore("visit-zero", 5);
        Customer customer = createCustomer(store, "010-1111-0002");

        VisitResult result = visitService.register(store.getId(), customer.getId(), visitForm(9, null)); // 9 × 5% = 0.45 → 0
        flushAndClear();

        assertEquals(0, result.earnedPoints());
        assertEquals(1, visitService.getVisitCount(store.getId(), customer.getId()));
        assertTrue(pointService.getRecentHistory(store.getId(), customer.getId()).isEmpty());
    }

    // ───────────── 방문 + 쿠폰 할인 ─────────────

    @Test
    @DisplayName("쿠폰을 쓰면 결제 금액에서 할인되고 할인 후 금액 기준으로 반올림해 적립한다")
    void couponDiscountIsAppliedBeforeEarning() {
        Store store = createStore("visit-coupon", 5);
        Customer customer = createCustomer(store, "010-1111-0020");
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("2천원 할인", 2000, LocalDate.now().plusDays(3)));
        flushAndClear();

        VisitForm form = visitForm(10010, null);
        form.setCouponId(couponId);
        VisitResult result = visitService.register(store.getId(), customer.getId(), form);
        flushAndClear();

        assertEquals(2000, result.discountAmount());
        assertEquals(8010, result.paidAmount());
        assertEquals(401, result.earnedPoints()); // 8010 × 5% = 400.5 → 반올림 401 (할인 전 10010 기준이면 501)
        assertEquals(401L, pointService.getBalance(store.getId(), customer.getId()));
        // 방문 기록에는 할인 후 실제 결제액이 저장된다
        assertEquals(8010, visitService.getRecentVisits(store.getId(), customer.getId()).get(0).getAmount());
        // 쿠폰은 사용 처리된다
        assertEquals("사용함", couponService.list(store.getId(), customer.getId()).get(0).getStatus());
    }

    @Test
    @DisplayName("할인액이 결제 금액보다 크면 결제 금액은 0원이고 적립은 없다")
    void discountLargerThanAmountStopsAtZero() {
        Store store = createStore("visit-coupon-big", 5);
        Customer customer = createCustomer(store, "010-1111-0021");
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("큰 할인", 5000, LocalDate.now().plusDays(3)));
        flushAndClear();

        VisitForm form = visitForm(3000, null);
        form.setCouponId(couponId);
        VisitResult result = visitService.register(store.getId(), customer.getId(), form);

        assertEquals(3000, result.discountAmount()); // 실제로 할인된 금액은 결제 금액까지
        assertEquals(0, result.paidAmount());
        assertEquals(0, result.earnedPoints());
    }

    @Test
    @DisplayName("이미 쓴 쿠폰으로는 방문을 등록할 수 없고 방문도 저장되지 않는다")
    void usedCouponRejectsVisit() {
        Store store = createStore("visit-coupon-used", 5);
        Customer customer = createCustomer(store, "010-1111-0022");
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("한 번만", 1000, LocalDate.now().plusDays(3)));
        flushAndClear();
        VisitForm first = visitForm(5000, null);
        first.setCouponId(couponId);
        visitService.register(store.getId(), customer.getId(), first);
        flushAndClear(); // 실제 서비스에서는 요청마다 새 트랜잭션이므로 같은 상태를 만든다

        VisitForm second = visitForm(5000, null);
        second.setCouponId(couponId);
        var e = assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), customer.getId(), second));
        assertEquals("이미 사용한 쿠폰입니다.", e.getMessage());
        flushAndClear();

        assertEquals(1L, visitService.getVisitCount(store.getId(), customer.getId()));
        assertEquals("사용함", couponService.list(store.getId(), customer.getId()).get(0).getStatus());
    }

    @Test
    @DisplayName("기한이 지난 쿠폰이나 다른 고객의 쿠폰으로는 방문을 등록할 수 없다")
    void expiredOrForeignCouponRejectsVisit() {
        Store store = createStore("visit-coupon-bad", 5);
        Customer me = createCustomer(store, "010-1111-0023");
        Customer other = createCustomer(store, "010-1111-0024");
        Coupon expired = new Coupon(me, "만료", 1000, LocalDate.now().minusDays(1));
        em.persist(expired);
        Long otherCoupon = couponService.issue(store.getId(), other.getId(), couponForm("남의 쿠폰", 1000, LocalDate.now().plusDays(3)));
        flushAndClear();

        VisitForm withExpired = visitForm(5000, null);
        withExpired.setCouponId(expired.getId());
        var e1 = assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), me.getId(), withExpired));
        assertEquals("기한이 지난 쿠폰입니다.", e1.getMessage());

        VisitForm withForeign = visitForm(5000, null);
        withForeign.setCouponId(otherCoupon);
        assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), me.getId(), withForeign));
        flushAndClear();

        assertEquals(0L, visitService.getVisitCount(store.getId(), me.getId()));
        assertEquals("사용 가능", couponService.list(store.getId(), other.getId()).get(0).getStatus()); // 남의 쿠폰은 그대로
    }

    // ───────────── 방문 + 포인트 사용 ─────────────

    @Test
    @DisplayName("포인트를 쓰면 1P = 1원으로 결제 금액에서 빠지고, 빠진 금액 기준으로 적립하며 잔액에 반영된다")
    void usedPointsReducePaidAmountAndBalance() {
        Store store = createStore("point-use", 10);
        Customer customer = createCustomer(store, "010-1111-0003");
        visitService.register(store.getId(), customer.getId(), visitForm(10000, null)); // 1000P 적립
        flushAndClear();

        VisitForm form = visitForm(5000, null);
        form.setUsedPoints(300);
        VisitResult result = visitService.register(store.getId(), customer.getId(), form);
        flushAndClear();

        assertEquals(300, result.usedPoints());
        assertEquals(4700, result.paidAmount());
        assertEquals(470, result.earnedPoints()); // 4700 × 10%
        assertEquals(1000L - 300 + 470, pointService.getBalance(store.getId(), customer.getId()));
        assertEquals(1, visitService.getRecentVisits(store.getId(), customer.getId()).stream()
                .filter(v -> v.getUsedPoint() == 300).count()); // 방문 기록에도 사용 포인트가 남는다
        assertEquals(3, pointService.getRecentHistory(store.getId(), customer.getId()).size()); // 첫 방문 적립, 두 번째 방문의 사용·적립
    }

    @Test
    @DisplayName("잔액보다 많이 사용하려 하면 거부하고 방문도 잔액도 그대로다")
    void usingMoreThanBalanceIsRejected() {
        Store store = createStore("point-over", 10);
        Customer customer = createCustomer(store, "010-1111-0004");
        visitService.register(store.getId(), customer.getId(), visitForm(10000, null)); // 1000P
        flushAndClear();

        VisitForm form = visitForm(5000, null);
        form.setUsedPoints(1001);
        var e = assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), customer.getId(), form));
        assertTrue(e.getMessage().contains("부족"));
        flushAndClear();

        assertEquals(1000L, pointService.getBalance(store.getId(), customer.getId()));
        assertEquals(1L, visitService.getVisitCount(store.getId(), customer.getId()));
    }

    @Test
    @DisplayName("잔액 전부를 쓸 수 있고, 결제 금액보다 많은 포인트와 음수 포인트는 거부한다")
    void pointLimits() {
        Store store = createStore("point-edge", 10);
        Customer customer = createCustomer(store, "010-1111-0005");
        visitService.register(store.getId(), customer.getId(), visitForm(50000, null)); // 5000P
        flushAndClear();

        VisitForm overAmount = visitForm(3000, null);
        overAmount.setUsedPoints(3001); // 잔액은 충분하지만 결제 금액(3000원)보다 큼
        var e1 = assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), customer.getId(), overAmount));
        assertTrue(e1.getMessage().contains("결제 금액"));

        VisitForm negative = visitForm(3000, null);
        negative.setUsedPoints(-5);
        assertThrows(IllegalArgumentException.class,
                () -> visitService.register(store.getId(), customer.getId(), negative));

        VisitForm exact = visitForm(5000, null);
        exact.setUsedPoints(5000); // 잔액 전부, 결제 금액 전부
        VisitResult result = visitService.register(store.getId(), customer.getId(), exact);
        flushAndClear();

        assertEquals(0, result.paidAmount());
        assertEquals(0, result.earnedPoints());
        assertEquals(0L, pointService.getBalance(store.getId(), customer.getId()));
    }

    @Test
    @DisplayName("쿠폰과 포인트를 함께 쓰면 쿠폰 할인 후 금액에서 포인트가 빠지고 그 금액 기준으로 적립한다")
    void couponAndPointsTogether() {
        Store store = createStore("coupon-and-points", 10);
        Customer customer = createCustomer(store, "010-1111-0025");
        visitService.register(store.getId(), customer.getId(), visitForm(10000, null)); // 1000P
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("2천원 할인", 2000, LocalDate.now().plusDays(3)));
        flushAndClear();

        VisitForm form = visitForm(10000, "쿠폰과 포인트");
        form.setCouponId(couponId);
        form.setUsedPoints(1000);
        VisitResult result = visitService.register(store.getId(), customer.getId(), form);
        flushAndClear();

        assertEquals(2000, result.discountAmount());
        assertEquals(1000, result.usedPoints());
        assertEquals(7000, result.paidAmount()); // 10000 - 2000 - 1000
        assertEquals(700, result.earnedPoints()); // 7000 × 10%
        assertEquals(700L, pointService.getBalance(store.getId(), customer.getId())); // 1000 - 1000 + 700
        assertEquals("사용함", couponService.list(store.getId(), customer.getId()).get(0).getStatus());
    }

    // ───────────── 쿠폰 발급 ─────────────

    @Test
    @DisplayName("쿠폰을 발급하면 목록에 나오고, 만료일이 과거면 발급을 거부한다")
    void issueCoupon() {
        Store store = createStore("coupon-issue", 5);
        Customer customer = createCustomer(store, "010-1111-0006");

        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("아메리카노 1잔", 3000, LocalDate.now().plusDays(7)));
        flushAndClear();

        var coupons = couponService.list(store.getId(), customer.getId());
        assertEquals(1, coupons.size());
        assertEquals(couponId, coupons.get(0).getId());
        assertEquals("사용 가능", coupons.get(0).getStatus());

        assertThrows(IllegalArgumentException.class, () -> couponService.issue(
                store.getId(), customer.getId(), couponForm("지난 쿠폰", 1000, LocalDate.now().minusDays(1))));
    }

    // ───────────── 매장 격리·삭제 고객 ─────────────

    @Test
    @DisplayName("다른 매장의 고객에게는 방문(쿠폰·포인트 포함)·쿠폰 발급·조회를 할 수 없다")
    void otherStoreCustomerIsIsolated() {
        Store mine = createStore("iso-mine", 5);
        Store other = createStore("iso-other", 5);
        Customer theirs = createCustomer(other, "010-1111-0009");
        visitService.register(other.getId(), theirs.getId(), visitForm(10000, null)); // 500P
        Long theirCoupon = couponService.issue(other.getId(), theirs.getId(), couponForm("남의 쿠폰", 1000, LocalDate.now().plusDays(3)));
        flushAndClear();

        Long myStore = mine.getId();
        Long theirId = theirs.getId();
        VisitForm plain = visitForm(1000, null);
        VisitForm withPoints = visitForm(1000, null);
        withPoints.setUsedPoints(1);
        VisitForm withCoupon = visitForm(1000, null);
        withCoupon.setCouponId(theirCoupon);
        assertThrows(IllegalArgumentException.class, () -> visitService.register(myStore, theirId, plain));
        assertThrows(IllegalArgumentException.class, () -> visitService.register(myStore, theirId, withPoints));
        assertThrows(IllegalArgumentException.class, () -> visitService.register(myStore, theirId, withCoupon));
        assertThrows(IllegalArgumentException.class, () -> pointService.getBalance(myStore, theirId));
        assertThrows(IllegalArgumentException.class, () -> couponService.issue(myStore, theirId, couponForm("침범", 1000, LocalDate.now().plusDays(3))));
        assertThrows(IllegalArgumentException.class, () -> detailService.getDetail(myStore, theirId));
        flushAndClear();

        // 남의 데이터는 변하지 않았다
        assertEquals(500L, pointService.getBalance(other.getId(), theirId));
        assertEquals(1L, visitService.getVisitCount(other.getId(), theirId));
        assertEquals("사용 가능", couponService.list(other.getId(), theirId).get(0).getStatus());
    }

    @Test
    @DisplayName("삭제된 고객에게는 새 거래를 막고, 과거 이력은 그대로 남는다")
    void deletedCustomerRejectsNewTransactionsButKeepsHistory() {
        Store store = createStore("deleted-customer", 10);
        Customer customer = createCustomer(store, "010-1111-0010");
        visitService.register(store.getId(), customer.getId(), visitForm(10000, "메모")); // 1000P
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("쿠폰", 1000, LocalDate.now().plusDays(3)));
        flushAndClear();

        em.find(Customer.class, customer.getId()).softDelete();
        flushAndClear();

        Long storeId = store.getId();
        Long customerId = customer.getId();
        VisitForm plain = visitForm(1000, null);
        VisitForm withPoints = visitForm(1000, null);
        withPoints.setUsedPoints(1);
        VisitForm withCoupon = visitForm(1000, null);
        withCoupon.setCouponId(couponId);
        assertThrows(IllegalArgumentException.class, () -> visitService.register(storeId, customerId, plain));
        assertThrows(IllegalArgumentException.class, () -> visitService.register(storeId, customerId, withPoints));
        assertThrows(IllegalArgumentException.class, () -> visitService.register(storeId, customerId, withCoupon));
        assertThrows(IllegalArgumentException.class, () -> couponService.issue(storeId, customerId, couponForm("신규", 1000, LocalDate.now().plusDays(3))));
        flushAndClear();

        // 과거 이력(방문 1건, 적립 1건, 쿠폰 1건)은 DB에 그대로 있다
        assertEquals(1L, count("Visit", customerId));
        assertEquals(1L, count("PointHistory", customerId));
        assertEquals(1L, count("Coupon", customerId));
    }

    // ───────────── 폐업 매장 ─────────────

    @Test
    @DisplayName("폐업 매장에는 방문 등록(쿠폰·포인트 포함)과 쿠폰 발급이 거부되고 과거 이력은 그대로 남는다")
    void closedStoreRejectsNewTransactionsButKeepsHistory() {
        Store store = createStore("closed-store", 10);
        Customer customer = createCustomer(store, "010-1111-0030");
        visitService.register(store.getId(), customer.getId(), visitForm(10000, null)); // 1000P
        Long couponId = couponService.issue(store.getId(), customer.getId(), couponForm("쿠폰", 1000, LocalDate.now().plusDays(3)));
        flushAndClear();

        em.find(Store.class, store.getId()).close(LocalDateTime.now());
        flushAndClear();

        Long storeId = store.getId();
        Long customerId = customer.getId();
        VisitForm plain = visitForm(1000, null);
        VisitForm withPoints = visitForm(1000, null);
        withPoints.setUsedPoints(1);
        VisitForm withCoupon = visitForm(1000, null);
        withCoupon.setCouponId(couponId);
        for (VisitForm form : new VisitForm[]{plain, withPoints, withCoupon}) {
            var e = assertThrows(ResponseStatusException.class, () -> visitService.register(storeId, customerId, form));
            assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        }
        var issue = assertThrows(ResponseStatusException.class, () -> couponService.issue(
                storeId, customerId, couponForm("신규", 1000, LocalDate.now().plusDays(3))));
        assertEquals(HttpStatus.CONFLICT, issue.getStatusCode());
        flushAndClear();

        // 폐업 전의 방문 1건, 적립 1건, 쿠폰 1건(미사용)은 그대로다
        assertEquals(1L, count("Visit", customerId));
        assertEquals(1L, count("PointHistory", customerId));
        assertEquals(1L, count("Coupon", customerId));
        assertEquals("사용 가능", couponService.list(storeId, customerId).get(0).getStatus());
    }

    @Test
    @DisplayName("이미 읽어 둔 매장 정보가 낡았어도 DB의 최신 폐업 여부로 거부한다")
    void closedStoreIsDetectedEvenWhenLoadedStoreIsStale() {
        Store store = createStore("closed-stale", 10);
        Customer customer = createCustomer(store, "010-1111-0031");
        flushAndClear();

        Store loaded = em.find(Store.class, store.getId()); // 영업 중으로 읽어 둔 상태(요청 앞부분에서 읽은 것과 같다)
        assertFalse(loaded.isClosed());
        // 다른 요청이 그 사이 폐업 처리했다고 가정 (읽어 둔 엔티티는 모르는 채로 DB만 바뀐다)
        em.createQuery("UPDATE Store s SET s.closedAt = :at WHERE s.id = :id")
                .setParameter("at", LocalDateTime.now()).setParameter("id", store.getId()).executeUpdate();

        Long storeId = store.getId();
        Long customerId = customer.getId();
        assertFalse(loaded.isClosed()); // 읽어 둔 엔티티는 여전히 영업 중으로 보인다
        var e = assertThrows(ResponseStatusException.class,
                () -> visitService.register(storeId, customerId, visitForm(1000, null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    // ───────────── 상세 화면 묶음 ─────────────

    @Test
    @DisplayName("상세 화면용 묶음에 방문·잔액·내역·쿠폰이 함께 담긴다")
    void detailBundlesEverything() {
        Store store = createStore("detail-bundle", 5);
        Customer customer = createCustomer(store, "010-1111-0011");
        visitService.register(store.getId(), customer.getId(), visitForm(20000, "라떼")); // 1000P
        VisitForm second = visitForm(1000, null);
        second.setUsedPoints(200); // 결제 800원, 적립 40P
        visitService.register(store.getId(), customer.getId(), second);
        couponService.issue(store.getId(), customer.getId(), couponForm("쿠폰", 500, LocalDate.now().plusDays(3)));
        flushAndClear();

        LoyaltyDetail detail = detailService.getDetail(store.getId(), customer.getId());

        assertEquals(2, detail.visits().size());
        assertEquals(2L, detail.visitCount());
        assertEquals(1000L - 200 + 40, detail.pointBalance());
        assertEquals(3, detail.pointHistory().size()); // 적립, 사용, 적립
        assertEquals(1, detail.coupons().size());
    }

    // ───────────── 준비 도우미 ─────────────

    private Store createStore(String label, int pointRate) {
        Member owner = new Member(label + "@test.invalid", "unused-test-password", "사장님-" + label, Role.USER);
        em.persist(owner);
        Store store = new Store(owner, label, "카페", "테스트 주소", pointRate);
        em.persist(store);
        return store;
    }

    private Customer createCustomer(Store store, String phone) {
        Customer customer = new Customer(store, "테스트 고객", phone, null, null, false, null);
        em.persist(customer);
        return customer;
    }

    private VisitForm visitForm(int amount, String memo) {
        VisitForm form = new VisitForm();
        form.setAmount(amount);
        form.setMenuMemo(memo);
        return form;
    }

    private CouponForm couponForm(String title, int discount, LocalDate expiresAt) {
        CouponForm form = new CouponForm();
        form.setTitle(title);
        form.setDiscount(discount);
        form.setExpiresAt(expiresAt);
        return form;
    }

    private long count(String entity, Long customerId) {
        return em.createQuery("SELECT COUNT(e) FROM " + entity + " e WHERE e.customer.id = :id", Long.class)
                .setParameter("id", customerId).getSingleResult();
    }

    /** 변경을 DB에 반영하고 캐시를 비운다. 서비스 호출마다 새 트랜잭션인 실제 요청 흐름과 같은 상태를 만든다. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
