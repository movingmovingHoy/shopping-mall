package com.example.shop.order.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 도메인. 테이블 이름은 order가 SQL 예약어라 orders지만, 클래스 이름은 Order로 한다.
 * - setter를 열지 않는다. 새 주문은 create()로만 만든다. (처음 상태는 항상 CREATED)
 * - 회원은 객체가 아니라 id만 가진다. 주문상품(OrderItem)도 이 객체가 들고 있지 않고,
 *   조회할 때 조인해서 DTO로 받는다. (MyBatis는 연관 객체를 자동으로 채워 주지 않는다)
 * - 결제(pay), 취소(cancel) 같은 상태 변경은 해당 기능을 만들 때 의미 있는 메서드로 추가한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private Long id;
    private Long memberId;
    private OrderStatus status;
    private int totalPrice;
    private String receiverName;
    private String receiverAddress;
    private LocalDateTime orderedAt;

    public static Order create(Long memberId, int totalPrice, String receiverName, String receiverAddress) {
        Order order = new Order();
        order.memberId = memberId;
        order.status = OrderStatus.CREATED;
        order.totalPrice = totalPrice;
        order.receiverName = receiverName;
        order.receiverAddress = receiverAddress;
        return order;
    }
}
