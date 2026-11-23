package com.team1.crm.admin;

import java.util.List;

/** 사장님 계정과 소유 매장 목록을 화면에 전달한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public record AdminOwnerRow(Long id, String email, String nickname, List<AdminStoreRow> stores) {
}
