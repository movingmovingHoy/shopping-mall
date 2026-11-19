package com.example.shop.order.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상세의 주문상품 한 줄 (조회 전용). order_item + product 조인 결과.
 * 가격은 product.price(현재 가격)가 아니라 order_item.order_price(주문 시점 가격)다.
 * 상품 이름은 order_item에 복사해 두지 않았으므로 product에서 가져온다. (상품을 물리 삭제하지 않으니 항상 조인된다)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItemDetail {

    private Long id;
    private Long productId;
    private String productName;
    private int orderPrice;
    private int quantity;
}
