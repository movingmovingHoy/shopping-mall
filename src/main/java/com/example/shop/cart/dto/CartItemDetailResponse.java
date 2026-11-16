package com.example.shop.cart.dto;

import com.example.shop.product.domain.ProductStatus;
import lombok.Getter;

/** 장바구니 조회 응답의 한 줄. linePrice(= 가격 x 수량)는 저장하지 않고 응답을 만들 때 계산한다. */
@Getter
public final class CartItemDetailResponse {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final int price;
    private final int quantity;
    private final long linePrice;
    private final ProductStatus productStatus;

    private CartItemDetailResponse(Long id, Long productId, String productName, int price,
                                   int quantity, long linePrice, ProductStatus productStatus) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.linePrice = linePrice;
        this.productStatus = productStatus;
    }

    public static CartItemDetailResponse from(CartItemDetail detail) {
        // int x int는 21억을 넘으면 넘쳐서 틀린 값이 되므로 long으로 바꿔서 곱한다
        long linePrice = (long) detail.getPrice() * detail.getQuantity();
        return new CartItemDetailResponse(detail.getId(), detail.getProductId(), detail.getProductName(),
                detail.getPrice(), detail.getQuantity(), linePrice, detail.getProductStatus());
    }
}
