package com.team1.crm.customer;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인한 회원의 소유 매장에 속한 고객을 JSON으로 조회한다.
 * 고객 검색과 페이지 이동에 필요한 데이터를 제공한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-20
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerApiController {
    private final CustomerService customerService;
    private final StoreService storeService;

    /**
     * 매장 소유권과 영업 상태를 확인하고 고객 페이지를 반환한다.
     * 삭제된 고객 제외와 검색 조건은 기존 서비스를 재사용한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerPageResponse> list (
            @LoginMember Member member,
            @RequestParam(name = "storeId") Long storeId,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page
            ) {

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (storeId <= 0 || page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "매장 ID와 페이지 번호를 확인해 주세요."
            );
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        selectedStore.requireOpen();

        String searchKeyword = keyword.strip();

        Page<Customer> customerPage =
                customerService.findCustomerPage(
                        selectedStore.getId(),
                        searchKeyword,
                        page
                );

        // 마지막 페이지를 초과하면 실제 마지막 페이지를 조회한다.
        if (customerPage.getTotalPages() > 0
                && customerPage.getNumber()
                >= customerPage.getTotalPages()) {

            customerPage = customerService.findCustomerPage(
                    selectedStore.getId(),
                    searchKeyword,
                    customerPage.getTotalPages() - 1
            );
        }

        CustomerPageResponse response =
                CustomerPageResponse.from(
                        selectedStore.getId(),
                        searchKeyword,
                        customerPage
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
