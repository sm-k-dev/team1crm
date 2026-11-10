package com.team1.crm.customer;

import com.team1.crm.auth.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 고객 등록 시 전화번호 정규화와 매장별 중복 검사 동작을 검증한다.
 * 실제 DB 대신 가짜 Repository를 사용한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-09
 */
class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerService customerService;
    private Store store;

    /**
     * 각 테스트에서 사용할 독립적인 Service와 가짜 객체를 준비한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerService = new CustomerService(customerRepository);

        store = mock(Store.class);
        when(store.getId()).thenReturn(900101L);
    }

    /**
     * 고객 등록에 사용할 정상 입력값을 만든다.
     *
     * @param phone 검사할 전화번호
     * @return 고객 등록 입력값
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    private CustomerForm validForm(String phone) {
        CustomerForm form = new CustomerForm();
        form.setName("김단골");
        form.setPhone(phone);
        form.setBirthMonth(5);
        form.setMemo("테스트 고객");
        form.setAgreeMarketing(true);
        return form;
    }

    /**
     * 저장 요청이 오면 지정한 ID의 고객을 반환하도록 설정한다.
     * 실제 DB 저장이나 자동 ID 생성은 수행하지 않는다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    private void prepareSuccessfulSave() {
        Customer savedCustomer = mock(Customer.class);
        when(savedCustomer.getId()).thenReturn(100L);

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);
    }

    /**
     * 서로 다른 전화번호 표기를 같은 형식으로 검사하고 저장하는지 확인한다.
     * 고객의 다른 입력값과 매장 연결도 함께 확인한다.
     *
     * @param inputPhone 사용자가 입력한 전화번호
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "01012345678",
            "010-1234-5678",
            "010 1234 5678",
            " 010-1234-5678 "
    })
    @DisplayName("전화번호 표기를 통일하여 중복 검사하고 고객을 저장한다")
    void normalizesPhoneBeforeCheckingAndSaving(String inputPhone) {
        CustomerForm form = validForm(inputPhone);
        prepareSuccessfulSave();

        Long customerId = customerService.registerCustomer(store, form);

        // 중복 검사에도 형식이 통일된 번호를 사용해야 한다.
        verify(customerRepository).existsByStore_IdAndPhone(
                900101L,
                "010-1234-5678"
        );

        // 실제로 저장 요청한 Customer를 꺼내 확인한다.
        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(customerRepository).save(captor.capture());

        Customer customer = captor.getValue();

        assertAll(
                () -> assertEquals(100L, customerId),
                () -> assertSame(store, customer.getStore()),
                () -> assertEquals("김단골", customer.getName()),
                () -> assertEquals(
                        "010-1234-5678",
                        customer.getPhone()
                ),
                () -> assertEquals(
                        Integer.valueOf(5),
                        customer.getBirthMonth()
                ),
                () -> assertEquals("테스트 고객", customer.getMemo()),
                () -> assertTrue(customer.isAgreeMarketing()),
                () -> assertNotNull(customer.getCreatedAt())
        );
    }

    /**
     * 전화번호가 없거나 형식이 잘못되면 DB 조회·저장 전에 거부하는지 확인한다.
     *
     * @param inputPhone 잘못된 전화번호
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "010123",
            "010123456789",
            "01112345678",
            "010-12ab-5678",
            "010/1234/5678"
    })
    @DisplayName("잘못된 전화번호는 Repository 호출 전에 거부한다")
    void invalidPhoneFailsBeforeRepositoryCall(String inputPhone) {
        CustomerForm form = validForm(inputPhone);

        assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(store, form)
        );

        verifyNoInteractions(customerRepository);
    }

    /**
     * 같은 매장에 같은 번호가 있으면 안내 오류를 발생시키고 저장하지 않는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("같은 매장에 같은 번호가 있으면 고객을 저장하지 않는다")
    void duplicatePhoneInSameStoreFails() {
        CustomerForm form = validForm("01012345678");

        when(customerRepository.existsByStore_IdAndPhone(
                900101L,
                "010-1234-5678"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(store, form)
        );

        assertEquals("이미 등록된 고객입니다.", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    /**
     * 중복 여부를 등록 대상 매장 ID로 조회하여 매장별로 판단하는지 확인한다.
     * 다른 매장에서는 같은 번호의 등록을 허용한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("다른 매장은 같은 전화번호라도 등록할 수 있다")
    void samePhoneInAnotherStoreCanBeRegistered() {
        CustomerForm form = validForm("01012345678");

        Store anotherStore = mock(Store.class);
        when(anotherStore.getId()).thenReturn(900103L);

        // 강남점에는 이미 등록된 번호라고 가정한다.
        when(customerRepository.existsByStore_IdAndPhone(
                900101L,
                "010-1234-5678"
        )).thenReturn(true);

        // 다른 매장에는 아직 없는 번호라고 가정한다.
        when(customerRepository.existsByStore_IdAndPhone(
                900103L,
                "010-1234-5678"
        )).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> customerService.registerCustomer(store, form)
        );

        verify(customerRepository, never()).save(any(Customer.class));

        prepareSuccessfulSave();

        Long customerId =
                customerService.registerCustomer(anotherStore, form);

        assertEquals(100L, customerId);

        verify(customerRepository).existsByStore_IdAndPhone(
                900103L,
                "010-1234-5678"
        );

        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(customerRepository).save(captor.capture());

        assertSame(anotherStore, captor.getValue().getStore());
    }

    /**
     * 매장과 고객 ID가 일치하는 고객을 반환하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("해당 매장에 속한 고객이면 상세 조회에 성공한다")
    void findsCustomerInSelectedStore() {
        Customer customer = mock(Customer.class);

        when(customerRepository.findByIdAndStore_Id(
                100L,
                900101L
        )).thenReturn(Optional.of(customer));

        Customer result = customerService.findCustomer(
                900101L,
                100L
        );

        assertSame(customer, result);

        verify(customerRepository).findByIdAndStore_Id(
                100L,
                900101L
        );
    }

    /**
     * 다른 매장 고객을 요청하면 해당 매장의 조회 결과가 없어 거부되는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("다른 매장 소속 고객은 상세 조회를 거부한다")
    void rejectsCustomerFromAnotherStore() {
        when(customerRepository.findByIdAndStore_Id(
                100L,
                900103L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.findCustomer(900103L, 100L)
        );

        assertEquals(
                "해당 매장의 고객을 찾을 수 없습니다.",
                exception.getMessage()
        );

        verify(customerRepository).findByIdAndStore_Id(
                100L,
                900103L
        );

        verify(customerRepository, never()).findById(any(Long.class));
    }

    /**
     * 존재하지 않는 고객 ID를 요청하면 조회를 거부하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("존재하지 않는 고객은 상세 조회를 거부한다")
    void rejectsMissingCustomer() {
        when(customerRepository.findByIdAndStore_Id(
                999999L,
                900101L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.findCustomer(900101L, 999999L)
        );

        assertEquals(
                "해당 매장의 고객을 찾을 수 없습니다.",
                exception.getMessage()
        );
    }

    /**
     * 자기 번호를 유지한 수정은 허용하고 정규화된 번호로 정보를 변경하는지 확인한다.
     * 기존 매장과 생성일은 유지되어야 한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("자기 번호를 유지한 채 고객 정보를 수정할 수 있다")
    void updatesCustomerWithOwnPhone() {
        Customer customer = new Customer(
                store,
                "기존 이름",
                "010-1234-5678",
                3,
                "기존 메모",
                false,
                null
        );
        var originalCreatedAt = customer.getCreatedAt();

        when(customerRepository.findByIdAndStore_Id(
                100L,
                900101L
        )).thenReturn(Optional.of(customer));

        when(customerRepository.existsByStore_IdAndPhoneAndIdNot(
                900101L,
                "010-1234-5678",
                100L
        )).thenReturn(false);

        CustomerForm form = validForm("01012345678");

        customerService.updateCustomer(900101L, 100L, form);

        assertAll(
                () -> assertEquals("김단골", customer.getName()),
                () -> assertEquals("010-1234-5678", customer.getPhone()),
                () -> assertEquals(
                        Integer.valueOf(5),
                        customer.getBirthMonth()
                ),
                () -> assertEquals("테스트 고객", customer.getMemo()),
                () -> assertTrue(customer.isAgreeMarketing()),
                () -> assertSame(store, customer.getStore()),
                () -> assertEquals(
                        originalCreatedAt,
                        customer.getCreatedAt()
                )
        );

        verify(customerRepository).existsByStore_IdAndPhoneAndIdNot(
                900101L,
                "010-1234-5678",
                100L
        );
    }

    /**
     * 다른 고객의 번호로 변경하려 하면 거부하고 기존 정보를 유지하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("다른 고객의 번호로 수정하면 거부하고 기존 정보를 유지한다")
    void rejectsUpdateWithAnotherCustomersPhone() {
        Customer customer = new Customer(
                store,
                "기존 이름",
                "010-1111-2222",
                3,
                "기존 메모",
                false,
                null
        );

        when(customerRepository.findByIdAndStore_Id(
                100L,
                900101L
        )).thenReturn(Optional.of(customer));

        when(customerRepository.existsByStore_IdAndPhoneAndIdNot(
                900101L,
                "010-1234-5678",
                100L
        )).thenReturn(true);

        CustomerForm form = validForm("01012345678");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.updateCustomer(
                        900101L,
                        100L,
                        form
                )
        );

        assertAll(
                () -> assertEquals(
                        "이미 등록된 고객입니다.",
                        exception.getMessage()
                ),
                () -> assertEquals("기존 이름", customer.getName()),
                () -> assertEquals("010-1111-2222", customer.getPhone()),
                () -> assertEquals(
                        Integer.valueOf(3),
                        customer.getBirthMonth()
                ),
                () -> assertEquals("기존 메모", customer.getMemo()),
                () -> assertFalse(customer.isAgreeMarketing())
        );

        verify(customerRepository, never()).save(any(Customer.class));
    }

    /**
     * 해당 매장에 속하지 않는 고객은 수정 전에 거부하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("다른 매장 고객은 중복 검사와 수정 전에 거부한다")
    void rejectsUpdateForCustomerFromAnotherStore() {
        when(customerRepository.findByIdAndStore_Id(
                100L,
                900103L
        )).thenReturn(Optional.empty());

        CustomerForm form = validForm("01012345678");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.updateCustomer(
                        900103L,
                        100L,
                        form
                )
        );

        assertEquals(
                "해당 매장의 고객을 찾을 수 없습니다.",
                exception.getMessage()
        );

        verify(customerRepository).findByIdAndStore_Id(
                100L,
                900103L
        );

        verifyNoMoreInteractions(customerRepository);
    }

    /**
     * 검색어가 없거나 공백뿐이면 선택 매장의 전체 고객 페이지를 조회하는지 확인한다.
     *
     * @param keyword 비어 있는 검색어
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("검색어가 없으면 선택 매장의 전체 고객 페이지를 조회한다")
    void blankKeywordLoadsAllCustomersPage(String keyword) {
        Page<Customer> expectedPage = new PageImpl<>(List.of());

        when(customerRepository.findAllByStore_Id(
                eq(900101L),
                any(Pageable.class)
        )).thenReturn(expectedPage);

        Page<Customer> result = customerService.findCustomerPage(
                900101L,
                keyword,
                0
        );

        assertSame(expectedPage, result);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(customerRepository).findAllByStore_Id(
                eq(900101L),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        assertAll(
                () -> assertEquals(0, pageable.getPageNumber()),
                () -> assertEquals(10, pageable.getPageSize()),
                () -> assertEquals(
                        Sort.by(Sort.Direction.ASC, "id"),
                        pageable.getSort()
                )
        );

        verifyNoMoreInteractions(customerRepository);
    }

    /**
     * 검색어의 앞뒤 공백을 제거하고 매장·페이지 조건을 전달하는지 확인한다.
     *
     * @param keyword 사용자가 입력한 검색어
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @ParameterizedTest
    @ValueSource(strings = {"단골", "5678"})
    @DisplayName("검색어의 앞뒤 공백을 제거하고 지정한 페이지를 검색한다")
    void trimsKeywordAndSearchesRequestedPage(String keyword) {
        Customer customer = mock(Customer.class);
        Page<Customer> expectedPage =
                new PageImpl<>(List.of(customer));

        when(customerRepository.searchCustomerPage(
                eq(900101L),
                eq(keyword),
                any(Pageable.class)
        )).thenReturn(expectedPage);

        Page<Customer> result = customerService.findCustomerPage(
                900101L,
                "  " + keyword + "  ",
                1
        );

        assertSame(expectedPage, result);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(customerRepository).searchCustomerPage(
                eq(900101L),
                eq(keyword),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        assertAll(
                () -> assertEquals(1, pageable.getPageNumber()),
                () -> assertEquals(10, pageable.getPageSize()),
                () -> assertEquals(10L, pageable.getOffset()),
                () -> assertEquals(
                        Sort.by(Sort.Direction.ASC, "id"),
                        pageable.getSort()
                )
        );

        verifyNoMoreInteractions(customerRepository);
    }

    /**
     * 음수 페이지 요청을 첫 페이지로 보정하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("음수 페이지는 첫 페이지로 처리한다")
    void negativePageUsesFirstPage() {
        when(customerRepository.findAllByStore_Id(
                eq(900101L),
                any(Pageable.class)
        )).thenReturn(new PageImpl<Customer>(List.of()));

        customerService.findCustomerPage(900101L, "", -1);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(customerRepository).findAllByStore_Id(
                eq(900101L),
                captor.capture()
        );

        assertEquals(0, captor.getValue().getPageNumber());
    }

    /**
     * 유효하지 않은 매장 ID는 Repository 조회 전에 거부하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Test
    @DisplayName("매장 ID가 없거나 양수가 아니면 페이지 조회를 거부한다")
    void invalidStoreIdRejectsPageLookup() {
        for (Long storeId : new Long[]{null, 0L, -1L}) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> customerService.findCustomerPage(
                            storeId,
                            "단골",
                            0
                    )
            );

            assertEquals(
                    "매장 정보를 확인해 주세요.",
                    exception.getMessage()
            );
        }

        verifyNoInteractions(customerRepository);
    }
}