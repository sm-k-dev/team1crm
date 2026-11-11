package com.team1.crm.customer;

import com.team1.crm.auth.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객 조회와 고객 관리 업무를 처리하는 서비스이다.
 * 매장별 고객 조회·등록·수정 및 소프트 삭제 기능을 제공한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 * 변경노트 2026-11-11 김순민: 소프트 삭제와 삭제 고객 업무 접근 제한 추가
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * 소유권 확인을 마친 매장의 고객을 소프트 삭제한다.
     * 삭제된 고객은 일반 조회·수정에서 제외하고 관련 이력은 보존한다.
     *
     * @param storeId 접근 권한을 확인한 매장의 ID
     * @param customerId 삭제할 고객의 ID
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Transactional
    public void deleteCustomer(Long storeId, Long customerId) {
        findCustomer(storeId, customerId).softDelete();
    }

    /**
     * 지정한 매장에 속한 고객 목록을 고객 ID 오름차순으로 조회한다.
     * 삭제 고객은 제외하며, 호출 전에 로그인한 사용자의 매장 접근 권한을 확인해야 한다.
     *
     * @param storeId 조회할 매장의 ID
     * @return 해당 매장의 고객 목록. 고객이 없으면 빈 목록
     * @throws IllegalArgumentException 매장 ID가 없거나 양수가 아닌 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-06
     */
    public List<Customer> findCustomers(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 ID는 양수여야 합니다.");
        }

        return customerRepository.findAllByStore_IdOrderByIdAsc(storeId);
    }

    /**
     * 전화번호에서 하이픈과 공백을 제거하고 휴대폰 번호 형식을 검증한다.
     * 검증을 통과하면 010-1234-5678 형식으로 변환하여 반환한다.
     *
     * @param phone 사용자가 입력한 전화번호
     * @return 하이픈을 포함한 형식으로 통일된 전화번호
     * @throws IllegalArgumentException 전화번호가 없거나 형식이 잘못된 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("고객 전화번호를 입력해 주세요.");
        }

        String digits = phone.replace("-", "")
                .replaceAll("\\s", "");

        if (!digits.matches("^010[0-9]{8}$")) {
            throw new IllegalArgumentException(
                    "전화번호는 010으로 시작하는 숫자 11자리여야 합니다."
            );
        }

        return digits.substring(0, 3)
                + "-"
                + digits.substring(3, 7)
                + "-"
                + digits.substring(7);
    }

    /**
     * 입력값으로 고객을 생성하여 지정한 매장에 등록한다.
     * 전화번호를 정규화하고 같은 매장 내 전화번호 중복을 검사한다.
     * 호출 전에 Form 입력값 검증과 매장 접근 권한 확인을 완료해야 한다.
     *
     * @param store 고객을 등록할 매장
     * @param form 검증을 마친 고객 입력값
     * @return 저장된 고객의 ID
     * @throws IllegalArgumentException 매장 정보가 없거나 전화번호가 잘못되었거나 중복인 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Transactional
    public Long registerCustomer(Store store, CustomerForm form) {
        if (store == null || store.getId() == null) {
            throw new IllegalArgumentException("등록할 매장 정보를 확인해 주세요.");
        }

        String phone = normalizePhone(form.getPhone());

        if (customerRepository.existsByStore_IdAndPhone(store.getId(), phone)) {
            throw new IllegalArgumentException("이미 등록된 고객입니다.");
        }

        Customer customer = new Customer(
                store,
                form.getName(),
                phone,
                form.getBirthMonth(),
                form.getMemo(),
                form.isAgreeMarketing(),
                null
        );

        Customer savedCustomer = customerRepository.save(customer);

        return savedCustomer.getId();
    }

    /**
     * 지정한 매장에 속한 고객 한 명을 조회한다.
     * 호출 전에 로그인한 회원의 매장 접근 권한을 확인해야 한다.
     *
     * @param storeId 접근 권한을 확인한 매장의 ID
     * @param customerId 조회할 고객의 ID
     * @return 해당 매장에 속한 고객
     * @throws IllegalArgumentException ID가 잘못되었거나 해당 매장의 고객이 아닌 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    public Customer findCustomer(Long storeId, Long customerId) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 정보를 확인해 주세요.");
        }

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 정보를 확인해 주세요.");
        }

        return customerRepository.findByIdAndStore_Id(customerId, storeId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "해당 매장의 고객을 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 지정한 매장에 속한 고객 정보를 수정한다.
     * 전화번호를 정규화하고 자신을 제외한 매장 내 번호 중복을 검사한다.
     * 호출 전에 Form 검증과 로그인 회원의 매장 접근 권한 확인을 완료해야 한다.
     *
     * @param storeId 접근 권한을 확인한 매장의 ID
     * @param customerId 수정할 고객의 ID
     * @param form 검증을 마친 고객 입력값
     * @throws IllegalArgumentException 고객을 찾을 수 없거나 전화번호가 잘못되었거나 중복인 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @Transactional
    public void updateCustomer(
            Long storeId,
            Long customerId,
            CustomerForm form
    ) {
        Customer customer = findCustomer(storeId, customerId);

        String phone = normalizePhone(form.getPhone());

        if (customerRepository.existsByStore_IdAndPhoneAndIdNot(
                storeId,
                phone,
                customerId
        )) {
            throw new IllegalArgumentException("이미 등록된 고객입니다.");
        }

        customer.update(
                form.getName(),
                phone,
                form.getBirthMonth(),
                form.getMemo(),
                form.isAgreeMarketing()
        );
    }

    /**
     * 지정한 매장의 고객을 이름 또는 전화번호 뒤 4자리로 검색한다.
     * 검색어가 없거나 공백뿐이면 해당 매장의 전체 고객을 조회한다.
     * 호출 전에 로그인한 회원의 매장 접근 권한을 확인해야 한다.
     *
     * @param storeId 접근 권한을 확인한 매장의 ID
     * @param keyword 검색어
     * @return 검색 결과 또는 해당 매장의 전체 고객 목록
     * @throws IllegalArgumentException 매장 ID가 잘못된 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    public List<Customer> searchCustomers(Long storeId, String keyword) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 정보를 확인해 주세요.");
        }

        String searchKeyword = keyword == null ? "" : keyword.strip();

        if (searchKeyword.isEmpty()) {
            return findCustomers(storeId);
        }

        return customerRepository.searchCustomers(storeId, searchKeyword);
    }

    /**
     * 지정한 매장의 고객을 검색하여 한 페이지에 10명씩 조회한다.
     * 검색어가 없으면 해당 매장의 전체 고객을 페이지 단위로 조회한다.
     * 호출 전에 로그인한 회원의 매장 접근 권한을 확인해야 한다.
     *
     * @param storeId 접근 권한을 확인한 매장의 ID
     * @param keyword 이름 또는 전화번호 뒤 4자리 검색어
     * @param page 조회할 페이지 번호. 첫 페이지는 0
     * @return 고객 목록과 전체 건수·페이지 정보
     * @throws IllegalArgumentException 매장 ID가 잘못된 경우
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    public Page<Customer> findCustomerPage(
            Long storeId,
            String keyword,
            int page
    ) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("매장 정보를 확인해 주세요.");
        }

        int pageNumber = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                10,
                Sort.by(Sort.Direction.ASC, "id")
        );

        String searchKeyword = keyword == null ? "" : keyword.strip();

        if (searchKeyword.isEmpty()) {
            return customerRepository.findAllByStore_Id(
                    storeId,
                    pageRequest
            );
        }

        return customerRepository.searchCustomerPage(
                storeId,
                searchKeyword,
                pageRequest
        );
    }
}
