package com.example.shop.cart.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 한 줄 (회원이 담은 상품 하나). Product와 같은 방식이다.
 * - setter를 열지 않는다. 새로 담을 때는 create(), 같은 상품을 또 담을 때는 addQuantity()로 바꾼다.
 * - 회원/상품은 객체가 아니라 id만 가진다. (테이블 컬럼 그대로)
 * - (member_id, product_id)는 DB에서 유니크다. 같은 상품을 또 담으면 줄을 새로 만들지 않고 수량을 더한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItem {

    private Long id;
    private Long memberId;
    private Long productId;
    private int quantity;

    public static CartItem create(Long memberId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.memberId = memberId;
        item.productId = productId;
        item.quantity = quantity;
        return item;
    }

    /** 이미 담긴 상품을 또 담았을 때 수량을 더한다 */
    public void addQuantity(int amount) {
        this.quantity += amount;
    }
}
