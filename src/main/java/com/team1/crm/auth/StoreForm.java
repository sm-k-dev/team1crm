
package com.team1.crm.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * + 매장 등록 요청 데이터 DTO
 * 역할: 사용자가 입력한 매장 이름, 업종, 주소,
 *      포인트 적립률을 전달받는 클래스입니다.
 *      StoreApiController에서 매장 등록 시 사용합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Getter // 각 필드의 값을 조회하는 Getter 메서드 자동 생성
@Setter // 각 필드의 값을 변경하는 Setter 메서드 자동 생성
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
