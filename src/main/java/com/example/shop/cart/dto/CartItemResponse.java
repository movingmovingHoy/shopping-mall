package com.example.shop.cart.dto;

import com.example.shop.cart.domain.CartItem;
import lombok.Getter;

/** 장바구니 담기 결과. 지금은 id, 상품, (합쳐진) 수량만 준다. 상품 이름/가격이 필요한 건 장바구니 조회에서 조인해서 만든다. */
@Getter
public final class CartItemResponse {

    private final Long id;
    private final Long productId;
    private final int quantity;

    private CartItemResponse(Long id, Long productId, int quantity) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(item.getId(), item.getProductId(), item.getQuantity());
    }
}
