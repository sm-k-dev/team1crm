package com.team1.crm.loyalty.coupon;
import com.team1.crm.customer.Customer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

// [Coupon] 쿠폰 (C 방문·포인트 담당) → coupon 테이블
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private int discount;          // 할인 금액(원)

    @Column(nullable = false)
    private LocalDate expiresAt;

    private LocalDateTime usedAt;  // null 이면 아직 안 씀

    public Coupon(Customer customer, String title, int discount, LocalDate expiresAt) {
        this.customer = customer;
        this.title = title;
        this.discount = discount;
        this.expiresAt = expiresAt;
    }

    // 화면 표시용 상태 (타임리프 ${cp.status})
    public String getStatus() {
        if (usedAt != null) return "사용함";
        return expiresAt.isBefore(LocalDate.now()) ? "기한 지남" : "사용 가능";
    }
}
