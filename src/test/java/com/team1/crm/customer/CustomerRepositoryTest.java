package com.team1.crm.customer;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.Store;
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
@Import(CustomerService.class)
class CustomerRepositoryTest {

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
