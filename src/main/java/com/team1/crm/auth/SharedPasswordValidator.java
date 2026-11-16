package com.team1.crm.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** 정규식을 복제하지 않고 PasswordPolicy에 검증을 위임한다. 작성자: 김순민 / 작성일: 2026-11-16 */
public class SharedPasswordValidator implements ConstraintValidator<SharedPassword, String> {
    /** 필드 오류는 Form에 반환하고 실제 저장에서도 같은 정책을 검사한다. 작성자: 김순민 / 작성일: 2026-11-16 */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        try {
            PasswordPolicy.validate(password);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
