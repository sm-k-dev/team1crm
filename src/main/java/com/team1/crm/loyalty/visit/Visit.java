package com.team1.crm.loyalty.visit;
import com.team1.crm.analytics.AnalyticsRepository;
import com.team1.crm.customer.Customer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// [Visit] 방문·결제 기록 (C 방문·포인트 담당) → visit 테이블
// 분석 쿼리(AnalyticsRepository)가 visited_at, amount 를 집계한다.
@Entity
@Table(indexes = @Index(name = "idx_visit_customer_time", columnList = "customer_id, visited_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    @Column(nullable = false)
    private int amount;          // 결제 금액(원)

    @Column(nullable = false)
    private int usedPoint;       // 이번 결제에 쓴 포인트

    @Column(length = 200)
    private String menuMemo;

    public Visit(Customer customer, LocalDateTime visitedAt, int amount, int usedPoint, String menuMemo) {
        this.customer = customer;
        this.visitedAt = visitedAt;
        this.amount = amount;
        this.usedPoint = usedPoint;
        this.menuMemo = menuMemo;
    }
}
