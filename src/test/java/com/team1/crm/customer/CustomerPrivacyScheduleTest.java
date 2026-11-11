package com.team1.crm.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 주요 정보 제거 배치의 명시적 활성화와 서비스 호출을 검증한다.
 * 작성자: 김순민
 * 작성일: 2026-11-11
 */
class CustomerPrivacyScheduleTest {
    /**
     * 기본 설정에서는 배치가 등록되지 않아 기존 앱에서 자동 실행되지 않는다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("활성화 설정이 없으면 개인정보 제거 배치를 등록하지 않는다")
    void disabledByDefault() {
        new ApplicationContextRunner().withUserConfiguration(CustomerPrivacySchedule.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(0, context.getBeansOfType(CustomerPrivacySchedule.class).size());
                });
    }

    /**
     * 활성화 시 예약 빈을 등록하고 실행하면 개인정보 제거 서비스를 호출한다.
     * 테스트의 cron은 '-'로 지정하여 실제 예약 실행을 막는다.
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @Test
    @DisplayName("명시적 활성화 후 배치 실행은 개인정보 제거 서비스를 호출한다")
    void enabledScheduleDelegatesToService() {
        CustomerPrivacyService service = mock(CustomerPrivacyService.class);
        new ApplicationContextRunner().withUserConfiguration(CustomerPrivacySchedule.class)
                .withBean(CustomerPrivacyService.class, () -> service)
                .withPropertyValues("app.customer.privacy.enabled=true", "app.customer.privacy.cron=-")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    CustomerPrivacySchedule schedule = context.getBean(CustomerPrivacySchedule.class);
                    verifyNoInteractions(service);
                    schedule.removeExpiredPersonalData();
                    verify(service).removeExpiredPersonalData(any(java.time.LocalDateTime.class));
                });
    }
}
