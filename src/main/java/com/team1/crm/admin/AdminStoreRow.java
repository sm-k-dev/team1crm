package com.team1.crm.admin;

/**
 * 관리자 매장 목록에 필요한 정보만 전달한다. 비밀번호 등 인증 정보는 포함하지 않는다.
 * 작성자: 김순민 / 작성일: 2026-11-11
 */
public record AdminStoreRow(Long id, String name, String category, String address,
                            Long ownerId, String ownerNickname, String ownerEmail, int pointRate) {
}
