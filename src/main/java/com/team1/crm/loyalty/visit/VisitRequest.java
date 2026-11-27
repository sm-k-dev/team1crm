package com.team1.crm.loyalty.visit;

import com.team1.crm.customer.Customer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** 성공한 방문 요청의 원래 결과를 보존한다. 원본 메모 등 요청 본문은 저장하지 않는다. 김순민, 2026-11-27. */
@Entity
@Table(name="visit_request", uniqueConstraints=@UniqueConstraint(
        name="uk_visit_request_customer_key",columnNames={"customer_id","request_key"}))
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class VisitRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @OnDelete(action=OnDeleteAction.CASCADE)
    @JoinColumn(name="customer_id",nullable=false) private Customer customer;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @OnDelete(action=OnDeleteAction.CASCADE)
    @JoinColumn(name="visit_id",nullable=false) private Visit visit;
    @Column(name="request_key",nullable=false,length=36) private String requestKey;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(nullable=false) private int earnedPoints;
    @Column(nullable=false) private int discountAmount;
    @Column(nullable=false) private int usedPoints;
    @Column(nullable=false) private int paidAmount;

    VisitRequest(Customer customer,Visit visit,String key,String fingerprint,VisitResult result) {
        this.customer=customer;this.visit=visit;this.requestKey=key;this.fingerprint=fingerprint;
        earnedPoints=result.earnedPoints();discountAmount=result.discountAmount();
        usedPoints=result.usedPoints();paidAmount=result.paidAmount();
    }
    boolean matches(String fingerprint){return this.fingerprint.equals(fingerprint);}
    VisitResult result(){return new VisitResult(visit.getId(),earnedPoints,discountAmount,usedPoints,paidAmount);}
}
