package com.team1.crm.customer;

import com.team1.crm.analytics.AnalyticsService;
import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.Store;
import com.team1.crm.loyalty.visit.Visit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 테스트 전용 메모리 DB에서 고객 Repository의 실제 조회 조건을 검증한다.
 * 개발용 MySQL 데이터는 사용하지 않는다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-10
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-repository-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@Import({CustomerService.class, CustomerPrivacyService.class, AnalyticsService.class})
class CustomerRepositoryTest {

    @Autowired
    private CustomerPrivacyService customerPrivacyService;

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * 삭제 및 개인정보 제거 전후의 과거 집계를 비교하고 현재 고객만 RFM에 포함되는지 확인한다.
     *
     * 작성자: 한상희
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("고객 삭제와 개인정보 제거 후에도 방문 집계는 유지되고 RFM에는 현재 고객만 남는다")
    void customerDeletionKeepsHistoricalAnalyticsAndExcludesCustomerFromRfm() {
        Store store = createStore("analytics-history");
        Customer removed = createCustomer(store, "삭제 대상 고객", "010-1111-5678");
        Customer active = createCustomer(store, "현재 고객", "010-2222-5678");
        Long storeId = store.getId();
        Long removedId = removed.getId();
        YearMonth month = YearMonth.of(2026, 7);
        entityManager.persist(new Visit(removed, month.atDay(1).atTime(12, 0), 15000, 0, null));
        entityManager.persist(new Visit(removed, month.atDay(2).atTime(13, 0), 5000, 0, null));
        entityManager.persist(new Visit(active, month.atDay(1).atTime(12, 0), 10000, 0, null));
        entityManager.flush();

        var before = analyticsService.summarize(storeId, month);
        assertEquals(30000L, before.revenue());
        assertEquals(3L, before.visits());
        assertEquals(2L, before.customers());
        assertEquals(1L, before.repeatCustomers());
        assertEquals(50L, before.repeatRatePercent());
        assertEquals(2, analyticsService.summarizeRfm(storeId, month).rows().size());

        customerService.deleteCustomer(storeId, removedId);
        entityManager.flush();
        entityManager.clear();
        assertHistoryAndCurrentRfm(storeId, month, before);

        setDeletedAt(removedId, LocalDateTime.of(2026, 8, 11, 3, 0));
        assertEquals(1, customerPrivacyService.removeExpiredPersonalData(LocalDateTime.of(2026, 11, 11, 3, 0)));
        assertHistoryAndCurrentRfm(storeId, month, before);
    }

    /**
     * 과거 요약과 차트를 비교하고 RFM의 고객 목록·등급별 인원·점수를 검증한다.
     *
     * 작성자: 한상희
     * 작성일: 2026-11-11
     */
    private void assertHistoryAndCurrentRfm(Long storeId, YearMonth month, AnalyticsService.Summary before) {
        var after = analyticsService.summarize(storeId, month);
        var rfm = analyticsService.summarizeRfm(storeId, month);
        assertAll(
                () -> assertEquals(before.revenue(), after.revenue()),
                () -> assertEquals(before.visits(), after.visits()),
                () -> assertEquals(before.customers(), after.customers()),
                () -> assertEquals(before.repeatCustomers(), after.repeatCustomers()),
                () -> assertEquals(before.repeatRatePercent(), after.repeatRatePercent()),
                () -> assertArrayEquals(before.weekdayVisits(), after.weekdayVisits()),
                () -> assertArrayEquals(before.hourlyVisits(), after.hourlyVisits()),
                () -> assertEquals(before.monthlyTrend(), after.monthlyTrend()),
                () -> assertEquals(List.of(new AnalyticsService.RfmRow(
                        "현재 고객", "2026-07-01", 1, 10000, 5, 5, 5, "VIP")), rfm.rows()),
                () -> assertEquals(1, rfm.gradeCounts().stream().mapToInt(AnalyticsService.GradeCount::count).sum())
        );
    }

    /**
     * 보관 기간 경계와 월말 보정을 실제 DB의 날짜 계산으로 검증한다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "2026-08-11T03:00:00,2026-11-11T03:00:00,1",
            "2026-08-11T03:00:01,2026-11-11T03:00:00,0",
            "2026-11-30T03:00:00,2027-02-28T03:00:00,1",
            "2026-11-30T03:00:00,2027-02-28T02:59:59,0",
            "2026-01-31T03:00:00,2026-04-30T03:00:00,1",
            "2027-11-30T03:00:00,2028-02-29T03:00:00,1"
    })
    @DisplayName("정확히 달력 3개월이 지난 고객만 월말 경계를 포함해 처리한다")
    void privacyRetentionBoundary(String deleted, String processing, int expected) {
        Store store = createStore("privacy-boundary");
        Customer customer = createCustomer(store, "보관 고객", "010-1111-5678");
        Long id = customer.getId();
        setDeletedAt(id, java.time.LocalDateTime.parse(deleted));
        int affected = customerPrivacyService.removeExpiredPersonalData(java.time.LocalDateTime.parse(processing));
        Customer result = entityManager.find(Customer.class, id);
        assertEquals(expected, affected);
        assertEquals(expected == 1 ? "삭제된 고객" : "보관 고객", result.getName());
        assertEquals(expected == 1 ? null : "010-1111-5678", result.getPhone());
    }

    /**
     * 테스트 고객의 삭제 시각을 고정하여 실제 3개월을 기다리지 않고 검증한다.
     * @param customerId 테스트 고객 ID
     * @param deletedAt 설정할 삭제 시각
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    private void setDeletedAt(Long customerId, java.time.LocalDateTime deletedAt) {
        entityManager.flush();
        entityManager.createNativeQuery("update customer set deleted_at = :at where id = :id")
                .setParameter("at", deletedAt).setParameter("id", customerId).executeUpdate();
        entityManager.clear();
    }

    /**
     * 여러 고객의 정보 제거·미대상 보존·재실행과 거래 이력·고유 방문 고객 수 보존을 확인한다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("주요 정보만 제거하며 고객·거래·과거 집계를 보존하고 재실행은 건너뛴다")
    void privacyBatchPreservesRowsAndHistory() {
        Store store = createStore("privacy-history");
        Long storeId = store.getId();
        Customer first = createCustomer(store, "첫 고객", "010-1111-5678");
        first.update("첫 고객", "010-1111-5678", 7, "개인 메모", true);
        Customer second = createCustomer(store, "둘째 고객", "010-2222-5678");
        Customer active = createCustomer(store, "현재 고객", "010-3333-5678");
        Long firstId = first.getId();
        Long secondId = second.getId();
        Long activeId = active.getId();
        var createdAt = first.getCreatedAt();
        var visitedAt = java.time.LocalDateTime.of(2026, 7, 1, 12, 0);
        var visit = new com.team1.crm.loyalty.visit.Visit(first, visitedAt, 15000, 0, "음료");
        entityManager.persist(visit);
        entityManager.persist(new com.team1.crm.loyalty.visit.Visit(second, visitedAt, 5000, 0, "음료"));
        entityManager.persist(new com.team1.crm.loyalty.point.PointHistory(first, visit,
                com.team1.crm.loyalty.point.PointType.EARN, 750, visitedAt));
        entityManager.persist(new com.team1.crm.loyalty.coupon.Coupon(first, "음료 할인", 1000,
                java.time.LocalDate.of(2026, 12, 31)));
        entityManager.flush();
        // DB 정밀도에 맞춘 생성일을 비교한다.
        entityManager.refresh(first);
        createdAt = first.getCreatedAt();
        var originalCreatedAt = createdAt;
        var deletedAt = java.time.LocalDateTime.of(2026, 8, 11, 3, 0);
        var processedAt = java.time.LocalDateTime.of(2026, 11, 11, 3, 0);
        setDeletedAt(firstId, deletedAt);
        setDeletedAt(secondId, deletedAt);

        assertEquals(2, customerPrivacyService.removeExpiredPersonalData(processedAt));
        Customer retained = entityManager.find(Customer.class, firstId);
        assertAll(
                () -> assertEquals("삭제된 고객", retained.getName()),
                () -> assertNull(retained.getPhone()),
                () -> assertNull(retained.getBirthMonth()),
                () -> assertNull(retained.getMemo()),
                () -> assertFalse(retained.isAgreeMarketing()),
                () -> assertEquals(deletedAt, retained.getDeletedAt()),
                () -> assertEquals(processedAt, retained.getAnonymizedAt()),
                () -> assertEquals(originalCreatedAt, retained.getCreatedAt()),
                () -> assertEquals(storeId, retained.getStore().getId()),
                () -> assertNotNull(entityManager.find(Customer.class, secondId)),
                () -> assertEquals("현재 고객", entityManager.find(Customer.class, activeId).getName())
        );
        assertEquals(2L, entityManager.createQuery("select count(v) from Visit v where v.customer.store.id = :storeId", Long.class)
                .setParameter("storeId", storeId).getSingleResult());
        assertEquals(20000L, entityManager.createQuery("select sum(v.amount) from Visit v where v.customer.store.id = :storeId", Long.class)
                .setParameter("storeId", storeId).getSingleResult());
        assertEquals(2L, entityManager.createQuery("select count(distinct v.customer.id) from Visit v where v.customer.store.id = :storeId", Long.class)
                .setParameter("storeId", storeId).getSingleResult());
        assertEquals(1L, entityManager.createQuery("select count(p) from PointHistory p where p.customer.id = :id", Long.class)
                .setParameter("id", firstId).getSingleResult());
        assertEquals(1L, entityManager.createQuery("select count(c) from Coupon c where c.customer.id = :id", Long.class)
                .setParameter("id", firstId).getSingleResult());
        assertEquals(0, customerPrivacyService.removeExpiredPersonalData(processedAt.plusDays(1)));
        Customer rerun = entityManager.find(Customer.class, firstId);
        assertEquals(processedAt, rerun.getAnonymizedAt());
        assertTrue(customerRepository.findByIdAndStore_Id(firstId, storeId).isEmpty());
        assertFalse(customerRepository.existsByStore_IdAndPhone(storeId, "010-1111-5678"));
        // 여러 고객의 번호가 null이어도 기존 번호를 새 고객이 다시 사용할 수 있다.
        Customer replacement = createCustomer(store, "새 고객", "010-1111-5678");
        entityManager.flush();
        assertNotNull(replacement.getId());
    }

    /**
     * 소프트 삭제가 업무 조회에서만 고객을 제외하고 실제 고객과 과거 방문 금액은 보존하는지 확인한다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("소프트 삭제 후 고객 조회·검색·수정은 차단하고 과거 방문·매출은 보존한다")
    void softDeletePreservesHistoryAndHidesCustomer() {
        Store store = createStore("soft-delete");
        Customer customer = createCustomer(store, "삭제대상", "010-1111-5678");
        Long storeId = store.getId();
        Long customerId = customer.getId();
        entityManager.persist(new com.team1.crm.loyalty.visit.Visit(
                customer, java.time.LocalDateTime.of(2026, 10, 1, 12, 0), 15000, 0, "방문 기록"));
        entityManager.flush();
        entityManager.clear();

        customerService.deleteCustomer(storeId, customerId);
        entityManager.flush();
        entityManager.clear();

        Customer retained = entityManager.find(Customer.class, customerId);
        assertNotNull(retained);
        assertNotNull(retained.getDeletedAt());
        assertTrue(customerRepository.findByIdAndStore_Id(customerId, storeId).isEmpty());
        assertTrue(customerRepository.findAllByStore_IdOrderByIdAsc(storeId).isEmpty());
        assertEquals(0L, customerRepository.countByStoreId(storeId));
        assertTrue(customerRepository.searchCustomers(storeId, "삭제").isEmpty());
        assertTrue(customerRepository.searchCustomers(storeId, "5678").isEmpty());
        assertEquals(0L, customerRepository.findAllByStore_Id(storeId, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0L, customerRepository.searchCustomerPage(storeId, "5678", PageRequest.of(0, 10)).getTotalElements());
        assertThrows(IllegalArgumentException.class, () -> customerService.findCustomer(storeId, customerId));
        assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(storeId, customerId, new CustomerForm()));
        assertEquals(1L, entityManager.createQuery(
                "select count(v) from Visit v where v.customer.store.id = :storeId", Long.class)
                .setParameter("storeId", storeId).getSingleResult());
        assertEquals(15000L, entityManager.createQuery(
                "select sum(v.amount) from Visit v where v.customer.store.id = :storeId", Long.class)
                .setParameter("storeId", storeId).getSingleResult());
        assertTrue(customerRepository.existsByStore_IdAndPhone(storeId, "010-1111-5678"));
    }

    /** 다른 매장 고객을 삭제할 수 없고 정상 고객은 계속 조회되는지 확인한다.
     * 작성자: 김순민 / 작성일: 2026-11-11 */
    @Test
    @DisplayName("다른 매장의 고객 삭제를 거부하고 원본을 보존한다")
    void cannotSoftDeleteOtherStoresCustomer() {
        Store own = createStore("own-store");
        Store other = createStore("other-store");
        Customer customer = createCustomer(other, "다른 매장 고객", "010-1111-5678");
        Long customerId = customer.getId();
        assertThrows(IllegalArgumentException.class, () -> customerService.deleteCustomer(own.getId(), customerId));
        entityManager.clear();
        assertNull(entityManager.find(Customer.class, customerId).getDeletedAt());
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EntityManager entityManager;

    /**
     * 서로 다른 매장에 고객을 저장한 뒤 선택한 매장의 고객만 조회되는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("선택한 매장의 고객만 조회한다")
    void findsOnlyCustomersInSelectedStore() {
        // 준비: 고객이 속할 매장과 소유 회원을 실제 테스트 DB에 저장한다.
        Member owner = new Member(
                "owner@test.invalid",
                "unused-test-password",
                "테스트 사장님",
                Role.USER
        );
        entityManager.persist(owner);

        Store gangnam = new Store(
                owner,
                "테스트 강남점",
                "카페",
                "테스트 주소",
                5
        );

        Store hongdae = new Store(
                owner,
                "테스트 홍대점",
                "카페",
                "테스트 주소",
                5
        );

        entityManager.persist(gangnam);
        entityManager.persist(hongdae);

        Customer gangnamCustomer = new Customer(
                gangnam,
                "강남 고객",
                "010-1111-5678",
                null,
                null,
                false,
                null
        );

        Customer hongdaeCustomer = new Customer(
                hongdae,
                "홍대 고객",
                "010-2222-5678",
                null,
                null,
                false,
                null
        );

        entityManager.persist(gangnamCustomer);
        entityManager.persist(hongdaeCustomer);

        Long gangnamStoreId = gangnam.getId();
        Long gangnamCustomerId = gangnamCustomer.getId();

        // 변경을 DB에 반영하고, 이후 조회가 새로 이루어지도록 관리 객체를 비운다.
        entityManager.flush();
        entityManager.clear();

        // 실행: 실제 Repository로 강남점 고객을 조회한다.
        List<Customer> customers =
                customerRepository.findAllByStore_IdOrderByIdAsc(
                        gangnamStoreId
                );

        // 확인: 홍대 고객은 제외되고 강남 고객만 나와야 한다.
        assertAll(
                () -> assertEquals(1, customers.size()),
                () -> assertEquals(
                        List.of(gangnamCustomerId),
                        customers.stream()
                                .map(Customer::getId)
                                .toList()
                ),
                () -> assertEquals("강남 고객", customers.getFirst().getName())
        );
    }

    /**
     * 테스트용 회원과 소유 매장을 DB에 저장한다.
     * 회원의 이메일과 닉네임은 매장별로 구분하여 중복 제약을 만족한다.
     *
     * @param label 테스트 안에서 구분할 고유한 영문 이름
     * @return 저장된 매장
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     * 변경노트 2026-11-11 김순민: 테스트 회원 닉네임 중복 방지
     */
    private Store createStore(String label) {
        Member owner = new Member(
                label + "@test.invalid",
                "unused-test-password",
                "사장님-" + label,
                Role.USER
        );
        entityManager.persist(owner);

        Store store = new Store(
                owner,
                label,
                "카페",
                "테스트 주소",
                5
        );
        entityManager.persist(store);

        return store;
    }

    /**
     * 지정한 매장에 테스트 고객을 저장한다.
     *
     * @param store 고객이 속할 매장
     * @param name 고객 이름
     * @param phone 고객 전화번호
     * @return 저장된 고객
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    private Customer createCustomer(Store store, String name, String phone) {
        Customer customer = new Customer(
                store,
                name,
                phone,
                null,
                null,
                false,
                null
        );
        entityManager.persist(customer);

        return customer;
    }

    /**
     * 이름 일부 또는 전화번호 뒤 4자리가 일치해도 선택 매장의 고객만 검색하는지 확인한다.
     *
     * @param keyword 이름 일부 또는 전화번호 뒤 4자리
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @ParameterizedTest
    @ValueSource(strings = {"단골", "5678"})
    @DisplayName("이름·번호 검색 결과에 다른 매장 고객이 섞이지 않는다")
    void searchExcludesCustomersFromOtherStores(String keyword) {
        Store gangnam = createStore("gangnam");
        Store hongdae = createStore("hongdae");

        Customer expected = createCustomer(
                gangnam,
                "김단골",
                "010-1111-5678"
        );

        createCustomer(gangnam, "이새봄", "010-1111-9999");
        createCustomer(hongdae, "박단골", "010-2222-5678");

        Long storeId = gangnam.getId();
        Long customerId = expected.getId();

        entityManager.flush();
        entityManager.clear();

        List<Customer> result =
                customerRepository.searchCustomers(storeId, keyword);

        assertEquals(
                List.of(customerId),
                result.stream().map(Customer::getId).toList()
        );
    }

    /**
     * 전화번호 검색은 마지막 4자리 전체가 일치해야 하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("전화번호 뒤 2자리만 입력하면 번호 검색에 일치하지 않는다")
    void phoneSearchRequiresAllFourTrailingDigits() {
        Store store = createStore("gangnam");
        createCustomer(store, "김단골", "010-1111-5678");

        Long storeId = store.getId();

        entityManager.flush();
        entityManager.clear();

        List<Customer> result =
                customerRepository.searchCustomers(storeId, "78");

        assertTrue(result.isEmpty());
    }

    /**
     * 전체 목록과 검색 결과가 매장 조건을 유지하며 10명과 2명으로 나뉘는지 확인한다.
     *
     * @param searching 검색 쿼리 사용 여부
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("전체 목록과 검색 결과를 10명씩 나누고 다른 매장 고객은 제외한다")
    void splitsCustomersIntoPages(boolean searching) {
        Store gangnam = createStore("gangnam");
        Store hongdae = createStore("hongdae");

        List<Long> expectedIds = IntStream.rangeClosed(1, 12)
                .mapToObj(number -> createCustomer(
                        gangnam,
                        "단골" + number,
                        String.format("010-1111-%04d", number)
                ).getId())
                .toList();

        // 다른 매장에도 검색어가 일치하는 고객을 넣는다.
        createCustomer(hongdae, "다른 매장 단골", "010-2222-0001");

        Long storeId = gangnam.getId();

        entityManager.flush();
        entityManager.clear();

        PageRequest firstRequest = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.ASC, "id")
        );

        PageRequest secondRequest = PageRequest.of(
                1,
                10,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Customer> firstPage;
        Page<Customer> secondPage;

        if (searching) {
            firstPage = customerRepository.searchCustomerPage(
                    storeId, "단골", firstRequest
            );
            secondPage = customerRepository.searchCustomerPage(
                    storeId, "단골", secondRequest
            );
        } else {
            firstPage = customerRepository.findAllByStore_Id(
                    storeId, firstRequest
            );
            secondPage = customerRepository.findAllByStore_Id(
                    storeId, secondRequest
            );
        }

        assertAll(
                () -> assertEquals(12L, firstPage.getTotalElements()),
                () -> assertEquals(2, firstPage.getTotalPages()),
                () -> assertEquals(10, firstPage.getNumberOfElements()),
                () -> assertEquals(2, secondPage.getNumberOfElements()),
                () -> assertTrue(firstPage.hasNext()),
                () -> assertFalse(secondPage.hasNext()),
                () -> assertEquals(
                        expectedIds.subList(0, 10),
                        firstPage.getContent().stream()
                                .map(Customer::getId)
                                .toList()
                ),
                () -> assertEquals(
                        expectedIds.subList(10, 12),
                        secondPage.getContent().stream()
                                .map(Customer::getId)
                                .toList()
                )
        );
    }

    /**
     * 수정 Service로 변경한 고객 정보가 실제 테스트 DB에 반영되는지 확인한다.
     * 고객 ID·소속 매장·생성일은 유지되어야 한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("고객 수정 내용이 DB에 반영되고 소속 매장과 생성일은 유지된다")
    void customerUpdateIsPersisted() {
        Store store = createStore("update-store");

        Customer customer = createCustomer(
                store,
                "수정 전 이름",
                "010-1111-2222"
        );

        Long storeId = store.getId();
        Long customerId = customer.getId();

        entityManager.flush();
        entityManager.clear();

        // DB에 저장된 값을 기준으로 생성일을 기억한다.
        Customer beforeUpdate = customerRepository
                .findByIdAndStore_Id(customerId, storeId)
                .orElseThrow();

        var originalCreatedAt = beforeUpdate.getCreatedAt();

        entityManager.clear();

        CustomerForm form = new CustomerForm();
        form.setName("수정 후 이름");
        form.setPhone("01033334444");
        form.setBirthMonth(7);
        form.setMemo("변경한 메모");
        form.setAgreeMarketing(true);

        // 실제 Service를 통해 수정한다.
        customerService.updateCustomer(storeId, customerId, form);

        // JPA 변경 감지로 DB에 반영하고 관리 중인 객체를 비운다.
        entityManager.flush();
        entityManager.clear();

        // 메모리의 기존 객체가 아니라 DB에서 다시 조회한다.
        Customer updated = customerRepository
                .findByIdAndStore_Id(customerId, storeId)
                .orElseThrow();

        assertAll(
                () -> assertEquals(customerId, updated.getId()),
                () -> assertEquals("수정 후 이름", updated.getName()),
                () -> assertEquals("010-3333-4444", updated.getPhone()),
                () -> assertEquals(
                        Integer.valueOf(7),
                        updated.getBirthMonth()
                ),
                () -> assertEquals("변경한 메모", updated.getMemo()),
                () -> assertTrue(updated.isAgreeMarketing()),
                () -> assertEquals(storeId, updated.getStore().getId()),
                () -> assertEquals(
                        originalCreatedAt,
                        updated.getCreatedAt()
                )
        );
    }

    /**
     * 같은 매장 다른 고객의 전화번호로 수정하면 거부하고
     * DB에 저장된 기존 고객 정보를 유지하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("중복 번호로 수정하면 DB의 기존 고객 정보가 유지된다")
    void duplicatePhoneUpdateKeepsOriginalData() {
        Store store = createStore("duplicate-update");

        Customer target = createCustomer(
                store,
                "수정 전 이름",
                "010-1111-2222"
        );

        Customer other = createCustomer(
                store,
                "다른 고객",
                "010-3333-4444"
        );

        Long storeId = store.getId();
        Long targetId = target.getId();
        Long otherId = other.getId();

        entityManager.flush();
        entityManager.clear();

        CustomerForm form = new CustomerForm();
        form.setName("바뀌면 안 되는 이름");
        form.setPhone("01033334444");
        form.setBirthMonth(7);
        form.setMemo("저장되면 안 되는 메모");
        form.setAgreeMarketing(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.updateCustomer(
                        storeId,
                        targetId,
                        form
                )
        );

        assertEquals(
                "이미 등록된 고객입니다.",
                exception.getMessage()
        );

        // 관리 중인 객체를 비운 뒤 DB의 기존 값을 다시 확인한다.
        entityManager.clear();

        Customer unchanged = customerRepository
                .findByIdAndStore_Id(targetId, storeId)
                .orElseThrow();

        Customer unchangedOther = customerRepository
                .findByIdAndStore_Id(otherId, storeId)
                .orElseThrow();

        assertAll(
                () -> assertEquals("수정 전 이름", unchanged.getName()),
                () -> assertEquals("010-1111-2222", unchanged.getPhone()),
                () -> assertNull(unchanged.getBirthMonth()),
                () -> assertNull(unchanged.getMemo()),
                () -> assertFalse(unchanged.isAgreeMarketing()),
                () -> assertEquals("다른 고객", unchangedOther.getName()),
                () -> assertEquals(
                        "010-3333-4444",
                        unchangedOther.getPhone()
                ),
                () -> assertEquals(
                        2L,
                        customerRepository.countByStoreId(storeId)
                )
        );
    }
}
