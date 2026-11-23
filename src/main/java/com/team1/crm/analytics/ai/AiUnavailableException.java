package com.team1.crm.analytics.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * AI 연결 설정 또는 응답 오류를 서비스 이용 불가로 전달한다.
 *
 * 작성자 한상희
 * 작성일 2026-11-11
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiUnavailableException extends RuntimeException {
    /** 화면에 표시할 고정 안내 문구를 받는다. 작성자 한상희, 작성일 2026-11-11 */
    public AiUnavailableException(String userMessage) {
        super(userMessage);
    }
}
