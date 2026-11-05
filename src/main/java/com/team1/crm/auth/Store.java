package com.team1.crm.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// ============================================================
// [Store] 매장 (A 회원·매장 담당) → store 테이블
// ============================================================
// 사장님(회원) 1명 = 여러 매장 가능. 모든 고객·방문 데이터는 매장 단위로 나뉜다.
// ============================================================
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @OneToOne: 회원 1명 ↔ 매장 여러 개. store 테이블에 member_id 외래키가 생기고 UNIQUE 제약은 두지 않는다.
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "member_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String category;   // 업종 (예: 카페, 한식)

    @Column(length = 200)
    private String address;

    // 결제 금액의 몇 % 를 포인트로 줄지 (기본 5)
    @Column(nullable = false)
    private int pointRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Store(Member owner, String name, String category, String address, int pointRate) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.address = address;
        this.pointRate = pointRate;
        this.createdAt = LocalDateTime.now();
    }

    // ADMIN-only reassignment; DB FK ensures owner exists.
    public void changeOwner(Member owner) { this.owner = owner; }

    // 호출: StoreService.update(). 값만 바꾸면 트랜잭션 종료 시 Hibernate 변경 감지로 UPDATE 실행
    public void update(String name, String category, String address, int pointRate) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.pointRate = pointRate;
    }
}
