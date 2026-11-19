package com.example.shop.order.dto;

import com.example.shop.order.domain.OrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 목록의 한 줄 (조회 전용). orders 한 행 + 주문상품 개수.
 * 목록에는 주문상품 내용을 넣지 않는다. 상품까지 보려면 상세 조회(GET /api/orders/{id})를 쓴다.
 *   이유: orders와 order_item을 JOIN 하면 주문 1건이 상품 수만큼 여러 행이 되어서,
 *   LIMIT 10이 "주문 10건"이 아니라 "행 10개"가 되어 페이징이 깨진다.
 * 평평한 결과라 resultMap 없이 자동 매핑된다. (item_count → itemCount)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderSummary {

    private Long id;
    private OrderStatus status;
    private int totalPrice;
    private int itemCount;
    private LocalDateTime orderedAt;
}
