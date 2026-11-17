package com.example.shop.order.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문상품 한 줄. 주문 시점의 상품 가격을 orderPrice에 복사해 저장한다.
 * 나중에 상품 가격이 바뀌어도 과거 주문 금액은 그대로 유지되어야 하기 때문이다.
 * (그래서 조회할 때 product.price가 아니라 이 값을 쓴다)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private int orderPrice;
    private int quantity;

    public static OrderItem create(Long orderId, Long productId, int orderPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.orderId = orderId;
        item.productId = productId;
        item.orderPrice = orderPrice;
        item.quantity = quantity;
        return item;
    }
}
