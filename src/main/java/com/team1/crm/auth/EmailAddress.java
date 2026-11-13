package com.team1.crm.auth;

import java.util.Locale;

/** 가입 경로 간 이메일 비교 기준을 통일한다. 작성자: 김순민 / 작성일: 2026-11-13 */
public final class EmailAddress {
    private EmailAddress() { }

    /** 앞뒤 공백·대소문자만 통일하며 점·플러스 별칭은 변경하지 않는다. 작성자: 김순민 / 작성일: 2026-11-13 */
    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
