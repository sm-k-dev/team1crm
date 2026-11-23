package com.team1.crm.customer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 타 매장·삭제·없는 고객은 구분해 노출하지 않고 404로 응답한다. 김순민, 2026-11-23 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends IllegalArgumentException {
    public CustomerNotFoundException() {
        super("해당 매장의 고객을 찾을 수 없습니다.");
    }
}
