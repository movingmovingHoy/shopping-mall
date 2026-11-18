package com.example.shop.cart.mapper;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/** SQL은 resources/mapper/cart/CartItemMapper.xml에 있다. */
@Mapper
public interface CartItemMapper {

    /** 저장 후 DB가 만든 id가 cartItem.id에 채워진다 (useGeneratedKeys) */
    void save(CartItem cartItem);

    /** 이 회원의 장바구니에 이 상품이 이미 있는지 찾는다 */
    Optional<CartItem> findByMemberIdAndProductId(@Param("memberId") Long memberId,
                                                  @Param("productId") Long productId);

    /** id로 찾되 이 회원의 항목일 때만 나온다. 남의 항목이면 없는 것과 같다 (소유자 확인을 SQL 조건으로 한다) */
    Optional<CartItem> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    /** 이 회원의 장바구니를 상품 정보와 함께, 최근에 담은 순으로 한 페이지 조회한다 */
    List<CartItemDetail> findAllByMemberId(@Param("memberId") Long memberId,
                                           @Param("size") int size,
                                           @Param("offset") int offset);

    /** findAllByMemberId와 같은 조건의 전체 개수 (LIMIT 없이) */
    long countByMemberId(@Param("memberId") Long memberId);

    /** 이 회원 장바구니 전체 중 판매중 상품의 (가격 x 수량) 합계. 비어 있으면 0 */
    long sumOnSalePriceByMemberId(@Param("memberId") Long memberId);

    /** 장바구니 항목 한 줄을 지운다 (물리 삭제). 소유자 확인은 호출 전에 findByIdAndMemberId로 끝낸 상태여야 한다 */
    void deleteById(Long id);

    /** cartItem.quantity 값으로 DB 수량을 바꾼다. 도메인 메서드(addQuantity)로 바꾼 뒤 호출한다 */
    void updateQuantity(CartItem cartItem);

    /**
     * 장바구니 주문용: 이 회원의 장바구니 전체를 페이징 없이 가져온다.
     * 목록 API는 항상 페이징하지만, 주문은 "일부만" 주문할 수 없고 전부 한 번에 처리해야 하므로 예외다.
     */
    List<CartItem> findAllForOrder(Long memberId);

    /** 주문을 마친 뒤 이 회원의 장바구니를 모두 비운다 */
    void deleteAllByMemberId(Long memberId);
}
