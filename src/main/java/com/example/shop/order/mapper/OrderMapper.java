package com.example.shop.order.mapper;

import com.example.shop.order.domain.Order;
import com.example.shop.order.dto.OrderDetail;
import com.example.shop.order.dto.OrderSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/** SQL은 resources/mapper/order/OrderMapper.xml에 있다. */
@Mapper
public interface OrderMapper {

    /** 저장 후 DB가 만든 id가 order.id에 채워진다 (useGeneratedKeys). 이 id를 order_item 저장에 쓴다 */
    void save(Order order);

    /** 지금은 저장 결과를 확인하는 데 쓰고, 주문 상세/취소/결제 API에서도 쓸 예정이다 */
    Optional<Order> findById(Long id);

    /** 내 주문 목록 한 페이지 (최근 주문 순). 주문상품 내용은 없고 개수만 있다 */
    List<OrderSummary> findAllByMemberId(@Param("memberId") Long memberId,
                                         @Param("size") int size,
                                         @Param("offset") int offset);

    /** findAllByMemberId와 같은 조건의 전체 개수 (LIMIT 없이) */
    long countByMemberId(@Param("memberId") Long memberId);

    /** 주문 상세 (주문상품 포함). member_id 조건이 소유자 확인이라 남의 주문은 empty */
    Optional<OrderDetail> findDetailByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);
}
