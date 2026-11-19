package com.example.shop.order.dto;

import lombok.Getter;

import java.util.List;

/** 주문 목록 응답 (한 페이지). ProductPageResponse와 같은 형태로 page/size/totalCount를 함께 준다. */
@Getter
public final class OrderPageResponse {

    private final List<OrderSummaryResponse> orders;
    private final int page;
    private final int size;
    private final long totalCount;

    private OrderPageResponse(List<OrderSummaryResponse> orders, int page, int size, long totalCount) {
        this.orders = orders;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
    }

    public static OrderPageResponse of(List<OrderSummaryResponse> orders, int page, int size, long totalCount) {
        return new OrderPageResponse(orders, page, size, totalCount);
    }
}
