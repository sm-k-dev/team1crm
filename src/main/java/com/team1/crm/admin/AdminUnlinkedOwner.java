package com.team1.crm.admin;

import java.time.LocalDateTime;

/** 매장 미보유 사장님의 화면용 정보이다. 작성자: 김순민 / 작성일: 2026-11-11 */
public record AdminUnlinkedOwner(Long id, String nickname, String email, LocalDateTime createdAt) {}
