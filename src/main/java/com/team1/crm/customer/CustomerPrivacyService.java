package com.team1.crm.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 소프트 삭제 고객의 3개월 보관 기간이 끝나면 주요 고객 정보를 제거한다.
 * 고객 행과 거래 관계는 유지하며, 실제 행 삭제를 수행하지 않는다.
 * 작성자: 김순민
 * 작성일: 2026-11-11
 */
@Service
@RequiredArgsConstructor
public class CustomerPrivacyService {
    private final CustomerRepository customerRepository;

    /**
     * 지정 시각에 보관 기간이 만료된 미처리 고객을 일괄 처리한다.
     * 단일 조건부 UPDATE로 중복 실행 시 이미 처리된 고객을 제외한다.
     * @param processedAt Asia/Seoul 기준 처리 시각
     * @return 주요 정보를 제거한 고객 수
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Transactional
    public int removeExpiredPersonalData(LocalDateTime processedAt) {
        if (processedAt == null) {
            throw new IllegalArgumentException("개인정보 제거 처리 시각이 필요합니다.");
        }
        return customerRepository.removeExpiredPersonalData(processedAt);
    }
}
