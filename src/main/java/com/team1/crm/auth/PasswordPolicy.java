package com.team1.crm.auth;

/**
 * + 공통 비밀번호 검증 정책
 * 역할: 일반 회원가입과 비밀번호 재설정에 같은 규칙을 적용합니다.
 *       8~20자, 영문·숫자·특수문자 각 1개 이상, 공백 사용 불가.
 *       규칙을 통과하지 못하면 IllegalArgumentException을 발생시킵니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */
public final class PasswordPolicy {
    private PasswordPolicy() { }

    private static final String PATTERN =
            "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,20}$";

    /**
     * 회원가입과 비밀번호 재설정에 동일한 검증을 수행합니다.
     * @param password 평문 비밀번호
     * @throws IllegalArgumentException 비밀번호가 정책에 맞지 않을 때
     */
    public static void validate(String password) {
        if (password == null || !password.matches(PATTERN)) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다. 공백은 사용할 수 없습니다."
            );
        }
    }
}
