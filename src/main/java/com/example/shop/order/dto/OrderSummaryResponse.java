package com.example.shop.order.dto;

import com.example.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/** 주문 목록 응답의 한 줄. itemCount는 주문상품 종류 수다. (수량 합계가 아니다) */
@Getter
public final class OrderSummaryResponse {

    private final Long id;
    private final OrderStatus status;
    private final int totalPrice;
    private final int itemCount;
    private final LocalDateTime orderedAt;

    private OrderSummaryResponse(Long id, OrderStatus status, int totalPrice, int itemCount, LocalDateTime orderedAt) {
        this.id = id;
        this.status = status;
        this.totalPrice = totalPrice;
        this.itemCount = itemCount;
        this.orderedAt = orderedAt;
    }

    public static OrderSummaryResponse from(OrderSummary summary) {
        return new OrderSummaryResponse(summary.getId(), summary.getStatus(), summary.getTotalPrice(),
                summary.getItemCount(), summary.getOrderedAt());
    }
}
