package com.example.shop.order.mapper;

import com.example.shop.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/** SQL은 resources/mapper/order/OrderMapper.xml에 있다. */
@Mapper
public interface OrderMapper {

    /** 저장 후 DB가 만든 id가 order.id에 채워진다 (useGeneratedKeys). 이 id를 order_item 저장에 쓴다 */
    void save(Order order);

    /** 지금은 저장 결과를 확인하는 데 쓰고, 주문 상세/취소/결제 API에서도 쓸 예정이다 */
    Optional<Order> findById(Long id);
}
