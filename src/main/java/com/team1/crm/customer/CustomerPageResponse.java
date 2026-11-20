package com.team1.crm.customer;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 고객 목록과 검색 페이지 정보를 JSON 응답으로 전달한다.
 * 페이지 번호는 서버 기준으로 0부터 시작한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-20
 */
public record CustomerPageResponse (
        Long storeId,
        String keyword,
        List<CustomerSummaryResponse> customers,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {

    /**
     * 조회한 고객 페이지를 화면에서 사용할 응답으로 변환한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    public static CustomerPageResponse from (
            Long storeId,
            String keyword,
            Page<Customer> customerPage
    ) {
        List<CustomerSummaryResponse> customers =
                customerPage.getContent() // 조회된 고객 목록을 꺼냄
                        .stream() // 고객을 한명씩 확인
                        .map(CustomerSummaryResponse::from) // customer를 CustomerSummaryResponse로 변환
                        .toList(); // 변환한 결과를 목록으로 만듦

        return new CustomerPageResponse(
                storeId,
                keyword,
                customers,
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements(),
                customerPage.getTotalPages(),
                customerPage.hasPrevious(),
                customerPage.hasNext()
        );
    }
}
