package com.team1.crm.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/** Form 검증에서도 인증 담당의 공통 비밀번호 정책을 재사용한다. 작성자: 김순민 / 작성일: 2026-11-16 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SharedPasswordValidator.class)
public @interface SharedPassword {
    String message() default "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 각각 포함해야 합니다. 공백은 사용할 수 없습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
