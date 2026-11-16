package com.team1.crm.loyalty;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 방문·포인트·쿠폰 API의 업무 규칙 위반을 400 응답으로 바꾼다.
 * 서비스가 던지는 IllegalArgumentException(잔액 부족, 이미 사용한 쿠폰, 매장 소유 아님 등)이
 * 처리되지 않으면 500으로 보이므로, 메시지를 담아 400으로 내려 준다.
 *
 * basePackages를 loyalty로 한정해서 다른 팀원의 컨트롤러 응답에는 영향을 주지 않는다.
 *
 * 작성자: 이재준
 */
@RestControllerAdvice(basePackages = "com.team1.crm.loyalty")
public class LoyaltyApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage()));
    }
}
