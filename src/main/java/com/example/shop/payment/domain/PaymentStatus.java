package com.example.shop.payment.domain;

/**
 * 결제 상태. DB의 payment.status(VARCHAR)에는 enum 이름이 그대로 저장된다.
 * 결제된 주문을 취소하면 COMPLETED -> CANCELED로 바뀐다.
 */
public enum PaymentStatus {
    COMPLETED,
    CANCELED
}
