package com.team1.crm.home;

/** 선택 매장 홈의 고객·방문·매출 요약이다. 작성자: 김순민 / 작성일: 2026-11-11 */
public record HomeSummary(long customerCount, long todayVisits, long monthRevenue) {}
