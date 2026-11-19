package com.example.shop.order.dto;

import com.example.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 응답. 주문 생성 응답(OrderResponse)에 주문 시각(orderedAt)이 더해진 형태다.
 * OrderResponse에 필드를 추가해 같이 쓰지 않은 이유: 생성 직후에는 orderedAt이 없어서(DB 기본값) 항상 null이 되기 때문이다.
 */
@Getter
public final class OrderDetailResponse {

    private final Long id;
    private final OrderStatus status;
    private final int totalPrice;
    private final String receiverName;
    private final String receiverAddress;
    private final LocalDateTime orderedAt;
    private final List<OrderItemResponse> items;

    private OrderDetailResponse(Long id, OrderStatus status, int totalPrice, String receiverName,
                                String receiverAddress, LocalDateTime orderedAt, List<OrderItemResponse> items) {
        this.id = id;
        this.status = status;
        this.totalPrice = totalPrice;
        this.receiverName = receiverName;
        this.receiverAddress = receiverAddress;
        this.orderedAt = orderedAt;
        this.items = items;
    }

    public static OrderDetailResponse from(OrderDetail detail) {
        List<OrderItemResponse> items = detail.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderDetailResponse(detail.getId(), detail.getStatus(), detail.getTotalPrice(),
                detail.getReceiverName(), detail.getReceiverAddress(), detail.getOrderedAt(), items);
    }
}
