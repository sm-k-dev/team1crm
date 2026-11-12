package com.team1.crm.auth;

/** 매장 이용 상태를 표시한다. 작성자: 김순민 / 작성일: 2026-11-11 */
public enum StoreStatus {
    OPEN("영업 중"), CLOSED("폐업");
    private final String displayName;
    /** 화면 표시명을 지정한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    StoreStatus(String displayName) { this.displayName = displayName; }
    /** 한글 상태명을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    public String getDisplayName() { return displayName; }
}
