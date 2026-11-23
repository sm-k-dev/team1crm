package com.team1.crm.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 스키마 적용 후 활성화하는 고객 주요 정보 제거 배치이다.
 * 기본 실행 시각은 매일 한국 시각 오전 3시이며 설정으로 변경할 수 있다.
 * 작성자: 김순민
 * 작성일: 2026-11-11
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.customer.privacy.enabled", havingValue = "true")
public class CustomerPrivacySchedule {
    private final CustomerPrivacyService customerPrivacyService;

    /**
     * 보관 기간이 끝난 소프트 삭제 고객의 주요 정보를 제거한다.
     * 실패하면 트랜잭션은 롤백되고 다음 예약 실행에서 재시도한다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Scheduled(cron = "${app.customer.privacy.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void removeExpiredPersonalData() {
        customerPrivacyService.removeExpiredPersonalData(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
}
