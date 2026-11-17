package com.example.shop.order.dto;

import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.util.List;

/**
 * 주문 생성 결과. 도메인(Order)을 그대로 반환하지 않고 필요한 값만 담는다.
 * 주문 시각(orderedAt)은 DB 기본값으로 채워져서 저장 직후의 Order에는 없으므로 여기서는 뺐다. (주문 상세 조회에서 준다)
 */
@Getter
public final class OrderResponse {

    private final Long id;
    private final OrderStatus status;
    private final int totalPrice;
    private final String receiverName;
    private final String receiverAddress;
    private final List<OrderItemResponse> items;

    private OrderResponse(Long id, OrderStatus status, int totalPrice, String receiverName,
                          String receiverAddress, List<OrderItemResponse> items) {
        this.id = id;
        this.status = status;
        this.totalPrice = totalPrice;
        this.receiverName = receiverName;
        this.receiverAddress = receiverAddress;
        this.items = items;
    }

    public static OrderResponse of(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalPrice(),
                order.getReceiverName(), order.getReceiverAddress(), items);
    }
}
