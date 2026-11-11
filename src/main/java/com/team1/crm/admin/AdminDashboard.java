package com.team1.crm.admin;

import java.util.List;

/** 관리자 운영 현황과 월별 가입 집계를 전달한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public record AdminDashboard(long ownerCount, long storeCount, long linkedOwnerCount,
                             long unlinkedOwnerCount, double connectionRate,
                             long newOwners, long newStores, long ownerChange, long storeChange,
                             List<Month> months, List<AdminUnlinkedOwner> unlinkedOwners) {
    /** 등록이 없는 월도 0으로 표시한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public record Month(String label, long owners, long stores) {}
}
