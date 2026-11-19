package com.example.shop.order.dto;

import com.example.shop.order.domain.OrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 (조회 전용). orders 1행 + 주문상품 N행을 한 객체로 묶은 결과.
 * JOIN 결과는 주문 정보가 상품 수만큼 반복되는 평평한 행들이라, 자동 매핑으로는 이 모양을 만들 수 없다.
 * 그래서 OrderMapper.xml의 resultMap(<id> + <collection>)으로 "같은 주문 id의 행들은 하나로 합치고,
 * 상품 컬럼들은 items 리스트에 하나씩 넣어라"라고 직접 알려 준다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderDetail {

    private Long id;
    private OrderStatus status;
    private int totalPrice;
    private String receiverName;
    private String receiverAddress;
    private LocalDateTime orderedAt;
    private List<OrderItemDetail> items;
}
