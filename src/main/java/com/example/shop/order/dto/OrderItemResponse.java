package com.example.shop.order.dto;

import com.example.shop.order.domain.OrderItem;
import lombok.Getter;

/** 주문 결과의 상품 한 줄. orderPrice는 상품의 현재 가격이 아니라 주문 시점에 저장한 가격이다. */
@Getter
public final class OrderItemResponse {

    private final Long productId;
    private final String productName;
    private final int orderPrice;
    private final int quantity;

    private OrderItemResponse(Long productId, String productName, int orderPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    /** 상품 이름은 order_item에 없으므로(가격만 복사) 주문하면서 이미 조회한 상품에서 받아 온다 */
    public static OrderItemResponse of(OrderItem item, String productName) {
        return new OrderItemResponse(item.getProductId(), productName, item.getOrderPrice(), item.getQuantity());
    }
}
