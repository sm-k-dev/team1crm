package com.team1.crm.customer;

import com.team1.crm.auth.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객 조회와 고객 관리 업무를 처리하는 서비스이다.
 * 현재는 매장별 고객 목록 조회 기능을 제공한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * 지정한 매장에 속한 고객 목록을 고객 ID 오름차순으로 조회한다.
     * 현재 매장 소유권 검증은 구현되어 있지 않으며,
     * 인증 연동 시 조회 전에 로그인한 사용자의 매장 접근 권한을 확인해야 한다.
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
}