package com.team1.crm.loyalty.point;

public enum PointType {
    EARN("적립"), USE("사용");

    private final String displayName;
    PointType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
