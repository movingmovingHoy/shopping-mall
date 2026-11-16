package com.example.shop.cart.dto;

import lombok.Getter;

import java.util.List;

/**
 * 장바구니 조회 응답 (한 페이지).
 * totalPrice는 이 페이지가 아니라 장바구니 전체 중 판매중(ON_SALE) 상품만의 합계다.
 *   (판매중지 상품은 주문할 수 없으므로 합계에서 뺀다. 목록에는 status와 함께 보여 줘서 사용자가 지울 수 있게 한다)
 */
@Getter
public final class CartResponse {

    private final List<CartItemDetailResponse> items;
    private final int page;
    private final int size;
    private final long totalCount;
    private final long totalPrice;

    private CartResponse(List<CartItemDetailResponse> items, int page, int size, long totalCount, long totalPrice) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
        this.totalPrice = totalPrice;
    }

    public static CartResponse of(List<CartItemDetailResponse> items, int page, int size,
                                  long totalCount, long totalPrice) {
        return new CartResponse(items, page, size, totalCount, totalPrice);
    }
}
