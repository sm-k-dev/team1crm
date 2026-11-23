package com.team1.crm.customer;

import java.time.LocalDate;

/**
 * 고객 목록 API에서 고객 한 명의 표시 정보를 전달한다.
 * 전화번호는 마스킹하고, 원본 번호와 메모는 응답에 포함하지 않는다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-20
 */
public record CustomerSummaryResponse (
        Long id,
        String name,
        String maskedPhone,
        Integer birthMonth,
        boolean agreeMarketing,
        LocalDate createdDate
) {
    /**
     * 고객 엔티티에서 목록 표시에 필요한 값만 복사한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-20
     */
    public static CustomerSummaryResponse from(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getMaskedPhone(),
                customer.getBirthMonth(),
                customer.isAgreeMarketing(),
                customer.getCreatedAt().toLocalDate()
        );
    }
}
