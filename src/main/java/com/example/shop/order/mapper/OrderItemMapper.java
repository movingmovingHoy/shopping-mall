package com.example.shop.order.mapper;

import com.example.shop.order.domain.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** SQL은 resources/mapper/order/OrderItemMapper.xml에 있다. */
@Mapper
public interface OrderItemMapper {

    /** 저장 후 DB가 만든 id가 orderItem.id에 채워진다 (useGeneratedKeys) */
    void save(OrderItem orderItem);

    /** 주문 하나의 주문상품 전체 (저장 결과 확인용. 주문 상세 조회는 3단계에서 조인 쿼리로 따로 만든다) */
    List<OrderItem> findAllByOrderId(Long orderId);
}
