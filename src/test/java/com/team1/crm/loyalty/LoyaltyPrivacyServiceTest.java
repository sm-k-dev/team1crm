package com.team1.crm.loyalty;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.Store;
import com.team1.crm.customer.Customer;
import com.team1.crm.customer.CustomerPrivacyService;
import com.team1.crm.loyalty.coupon.Coupon;
import com.team1.crm.loyalty.point.PointHistory;
import com.team1.crm.loyalty.point.PointType;
import com.team1.crm.loyalty.visit.Visit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 고객 개인정보 파기 때 방문 메모·쿠폰 제목 같은 자유 입력 글도 함께 정리되는지 검증한다.
 * 실제 파기 흐름(CustomerPrivacyService)을 테스트 전용 메모리 DB에서 그대로 실행한다.
 *
 * 확인하는 것: 지워야 할 것(방문 메모, 쿠폰 제목)이 지워지는지, 남겨야 할 것(결제 금액·방문일·사용 포인트·
 *              할인액·만료일·사용 이력·포인트 내역)이 그대로인지, 대상이 아닌 고객은 그대로인지, 반복 실행해도 같은지.
 *
 * 작성자: 이재준
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loyalty-privacy-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import({CustomerPrivacyService.class, LoyaltyPrivacyService.class})
class LoyaltyPrivacyServiceTest {

    private static final String PERSONAL_MEMO = "김단골님 010-1234-5678 단골";
    private static final LocalDateTime VISITED_AT = LocalDateTime.of(2026, 7, 1, 12, 0);

    @Autowired private EntityManager em;
    @Autowired private CustomerPrivacyService privacyService;

    /** 한 고객의 거래 이력 묶음(테스트에서 값을 다시 읽기 위해 ID를 들고 있다). */
    private record History(Long customerId, Long visitId, Long couponId, Long pointId) {}

    @Test
    @DisplayName("파기 대상 고객의 방문 메모와 쿠폰 제목은 지워지고, 결제 금액·방문일·포인트·할인액은 그대로 남는다")
    void freeTextIsClearedButPaymentDataIsKept() {
        Store store = createStore("privacy-clear");
        History expired = createHistory(store, "010-1111-0001");
        deletedMonthsAgo(expired.customerId(), 4);

        int removedCustomers = privacyService.removeExpiredPersonalData(LocalDateTime.now());
        em.clear();

        assertEquals(1, removedCustomers);
        // 지워진 것: 사람이 자유롭게 적은 글
        assertNull(em.find(Visit.class, expired.visitId()).getMenuMemo());
        assertEquals("삭제된 고객 쿠폰", em.find(Coupon.class, expired.couponId()).getTitle());
        // 남은 것: 결제·통계·이력 데이터
        Visit visit = em.find(Visit.class, expired.visitId());
        assertEquals(12000, visit.getAmount());
        assertEquals(VISITED_AT, visit.getVisitedAt());
        assertEquals(300, visit.getUsedPoint());
        Coupon coupon = em.find(Coupon.class, expired.couponId());
        assertEquals(2000, coupon.getDiscount());
        assertEquals(LocalDate.of(2026, 12, 31), coupon.getExpiresAt());
        assertNotNull(coupon.getUsedAt()); // 사용 이력 유지
        PointHistory point = em.find(PointHistory.class, expired.pointId());
        assertEquals(PointType.EARN, point.getType());
        assertEquals(600, point.getPoint());
        // 고객 정보는 파기 정책대로 지워졌다
        Customer customer = em.find(Customer.class, expired.customerId());
        assertEquals("삭제된 고객", customer.getName());
        assertNull(customer.getPhone());
        assertNotNull(customer.getAnonymizedAt());
    }

    @Test
    @DisplayName("아직 보관 기간 중인 삭제 고객과 활성 고객의 방문 메모·쿠폰 제목은 건드리지 않는다")
    void notExpiredCustomersAreUntouched() {
        Store store = createStore("privacy-untouched");
        History recentlyDeleted = createHistory(store, "010-1111-0002");
        deletedMonthsAgo(recentlyDeleted.customerId(), 1);          // 3개월이 지나지 않음
        History active = createHistory(store, "010-1111-0003");      // 삭제하지 않은 고객
        History expired = createHistory(store, "010-1111-0004");
        deletedMonthsAgo(expired.customerId(), 4);

        privacyService.removeExpiredPersonalData(LocalDateTime.now());
        em.clear();

        assertEquals(PERSONAL_MEMO, em.find(Visit.class, recentlyDeleted.visitId()).getMenuMemo());
        assertEquals("김단골님 생일 쿠폰", em.find(Coupon.class, recentlyDeleted.couponId()).getTitle());
        assertEquals(PERSONAL_MEMO, em.find(Visit.class, active.visitId()).getMenuMemo());
        assertEquals("김단골님 생일 쿠폰", em.find(Coupon.class, active.couponId()).getTitle());
        assertNull(em.find(Visit.class, expired.visitId()).getMenuMemo()); // 대상 고객만 정리됨
    }

    @Test
    @DisplayName("같은 파기를 다시 실행해도 결과가 같고, 처리한 행이 없으면 아무것도 바꾸지 않는다")
    void repeatedRunIsIdempotent() {
        Store store = createStore("privacy-repeat");
        History expired = createHistory(store, "010-1111-0005");
        deletedMonthsAgo(expired.customerId(), 4);

        assertEquals(1, privacyService.removeExpiredPersonalData(LocalDateTime.now()));
        em.clear();
        assertEquals(0, privacyService.removeExpiredPersonalData(LocalDateTime.now())); // 두 번째: 대상 고객 없음
        em.clear();

        assertNull(em.find(Visit.class, expired.visitId()).getMenuMemo());
        assertEquals("삭제된 고객 쿠폰", em.find(Coupon.class, expired.couponId()).getTitle());
        assertEquals(12000, em.find(Visit.class, expired.visitId()).getAmount());
    }

    @Test
    @DisplayName("이 기능이 생기기 전에 이미 개인정보가 제거된 고객의 남은 메모·제목도 다음 실행에서 정리된다")
    void alreadyAnonymizedCustomerIsHealedOnNextRun() {
        Store store = createStore("privacy-legacy");
        History legacy = createHistory(store, "010-1111-0006");
        // 예전 파기(고객 테이블만 지우던 시절)를 재현: 고객은 이미 anonymized_at 이 있지만 메모·제목은 남아 있다
        em.createQuery("UPDATE Customer c SET c.deletedAt = :d, c.anonymizedAt = :a, c.name = '삭제된 고객', c.phone = null WHERE c.id = :id")
                .setParameter("d", LocalDateTime.now().minusMonths(6)).setParameter("a", LocalDateTime.now().minusMonths(3))
                .setParameter("id", legacy.customerId()).executeUpdate();
        em.clear();
        assertEquals(PERSONAL_MEMO, em.find(Visit.class, legacy.visitId()).getMenuMemo()); // 정리 전 상태 확인
        em.clear();

        int removedCustomers = privacyService.removeExpiredPersonalData(LocalDateTime.now());
        em.clear();

        assertEquals(0, removedCustomers); // 고객은 이미 처리되어 대상이 아니지만
        assertNull(em.find(Visit.class, legacy.visitId()).getMenuMemo());          // 남은 글은 정리된다
        assertEquals("삭제된 고객 쿠폰", em.find(Coupon.class, legacy.couponId()).getTitle());
    }

    @Test
    @DisplayName("방문 메모가 원래 비어 있던 방문과 이미 정리된 제목은 그대로다")
    void emptyMemoStaysEmpty() {
        Store store = createStore("privacy-empty");
        Customer customer = createCustomer(store, "010-1111-0007");
        Visit visit = new Visit(customer, VISITED_AT, 5000, 0, null);
        em.persist(visit);
        Coupon coupon = new Coupon(customer, "삭제된 고객 쿠폰", 1000, LocalDate.of(2026, 12, 31));
        em.persist(coupon);
        em.flush();
        deletedMonthsAgo(customer.getId(), 4);

        privacyService.removeExpiredPersonalData(LocalDateTime.now());
        em.clear();

        assertNull(em.find(Visit.class, visit.getId()).getMenuMemo());
        assertEquals("삭제된 고객 쿠폰", em.find(Coupon.class, coupon.getId()).getTitle());
        assertEquals(5000, em.find(Visit.class, visit.getId()).getAmount());
    }

    // ───────────── 준비 도우미 ─────────────

    private Store createStore(String label) {
        Member owner = new Member(label + "@test.invalid", "unused-test-password", "사장님-" + label, Role.USER);
        em.persist(owner);
        Store store = new Store(owner, label, "카페", "테스트 주소", 5);
        em.persist(store);
        return store;
    }

    private Customer createCustomer(Store store, String phone) {
        Customer customer = new Customer(store, "김단골", phone, null, null, false, null);
        em.persist(customer);
        return customer;
    }

    /** 개인정보가 적힌 방문 메모·쿠폰 제목과 결제·포인트 이력을 가진 고객 한 명을 만든다. */
    private History createHistory(Store store, String phone) {
        Customer customer = createCustomer(store, phone);
        Visit visit = new Visit(customer, VISITED_AT, 12000, 300, PERSONAL_MEMO);
        em.persist(visit);
        Coupon coupon = new Coupon(customer, "김단골님 생일 쿠폰", 2000, LocalDate.of(2026, 12, 31));
        em.persist(coupon);
        em.flush();
        em.createQuery("UPDATE Coupon c SET c.usedAt = :at WHERE c.id = :id")
                .setParameter("at", VISITED_AT).setParameter("id", coupon.getId()).executeUpdate();
        PointHistory point = new PointHistory(customer, visit, PointType.EARN, 600, VISITED_AT);
        em.persist(point);
        em.flush();
        em.clear();
        return new History(customer.getId(), visit.getId(), coupon.getId(), point.getId());
    }

    private void deletedMonthsAgo(Long customerId, int months) {
        em.createQuery("UPDATE Customer c SET c.deletedAt = :d WHERE c.id = :id")
                .setParameter("d", LocalDateTime.now().minusMonths(months)).setParameter("id", customerId).executeUpdate();
        em.clear();
    }
}
