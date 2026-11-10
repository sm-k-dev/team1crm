
package com.team1.crm.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreForm {

    // 매장 이름
    private String name;

    // 업종 (카페, 음식점 등)
    private String category;

    // 매장 주소
    private String address;

    // 포인트 적립률 (기본 5%)
    private int pointRate = 5;
}
