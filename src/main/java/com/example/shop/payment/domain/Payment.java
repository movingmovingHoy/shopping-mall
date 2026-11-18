package com.example.shop.payment.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모의 결제. 외부 PG 연동 없이 "결제됐다"는 기록만 남긴다.
 * 주문 1건당 최대 1건이다. (payment.order_id는 DB에서 유니크)
 * 결제를 만드는 순간이 곧 결제 완료이므로, 새 결제의 상태는 항상 COMPLETED다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    private Long id;
    private Long orderId;
    private int amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;

    public static Payment create(Long orderId, int amount) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.status = PaymentStatus.COMPLETED;
        return payment;
    }
}
