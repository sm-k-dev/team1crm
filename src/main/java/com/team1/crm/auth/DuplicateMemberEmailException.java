package com.team1.crm.auth;

/** 일반·Google 가입에 관계없이 기존 이메일의 재가입을 알린다. 작성자: 김순민 / 작성일: 2026-11-13 */
public class DuplicateMemberEmailException extends IllegalArgumentException {
    public DuplicateMemberEmailException() {
        super("이미 가입된 이메일입니다. Google로 가입했다면 Google 로그인을 이용해 주세요.");
    }
}
