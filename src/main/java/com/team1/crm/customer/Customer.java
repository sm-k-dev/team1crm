package com.team1.crm.customer;
import com.team1.crm.auth.Store;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// ============================================================
// [Customer] 고객 (B 고객 담당) → customer 테이블
// ============================================================
// 전화번호는 원문으로 저장하되 화면에는 getMaskedPhone() 으로 가운데를 가린다.
// 같은 매장 안에서 전화번호가 겹치지 않도록 (store_id, phone) 묶음에 UNIQUE 제약을 건다.
// ============================================================
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_customer_phone", columnNames = {"store_id", "phone"}),
       indexes = @Index(name = "idx_customer_name", columnList = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 고객 N : 매장 1. 매장이 지워지면 DB 가 고객도 지운다 (ON DELETE CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 13)
    private String phone;          // 010-1234-5678

    private Integer birthMonth;    // 1~12, 생일 쿠폰용 (선택)

    @Column(length = 300)
    private String memo;

    // 마케팅 문자 수신 동의 여부. 동의한 고객에게만 문구를 보낸다는 규칙을 화면에 표시
    @Column(nullable = false)
    private boolean agreeMarketing;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Customer(Store store, String name, String phone, Integer birthMonth, String memo, boolean agreeMarketing, LocalDateTime createdAt) {
        this.store = store;
        this.name = name;
        this.phone = phone;
        this.birthMonth = birthMonth;
        this.memo = memo;
        this.agreeMarketing = agreeMarketing;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public void update(String name, String phone, Integer birthMonth, String memo, boolean agreeMarketing) {
        this.name = name;
        this.phone = phone;
        this.birthMonth = birthMonth;
        this.memo = memo;
        this.agreeMarketing = agreeMarketing;
    }

    // 010-1234-5678 → 010-****-5678. 타임리프에서 ${c.maskedPhone} 으로 읽는다 (getter 이름 규칙)
    public String getMaskedPhone() {
        if (phone == null || phone.length() < 4) return phone;
        String[] p = phone.split("-");
        return p.length == 3 ? p[0] + "-****-" + p[2] : "****" + phone.substring(phone.length() - 4);
    }
}
