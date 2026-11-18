package com.example.shop.order.domain;

/**
 * 주문 상태. DB의 orders.status(VARCHAR)에는 enum 이름이 그대로 저장된다.
 * CREATED(주문 생성) -> PAID(결제 완료) -> CANCELED(취소). 결제 전/후 모두 취소할 수 있다.
 */
public enum OrderStatus {
    CREATED,
    PAID,
    CANCELED
}
