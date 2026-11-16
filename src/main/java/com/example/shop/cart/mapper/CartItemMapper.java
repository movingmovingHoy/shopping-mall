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

    /** cartItem.quantity 값으로 DB 수량을 바꾼다. 도메인 메서드(addQuantity)로 바꾼 뒤 호출한다 */
    void updateQuantity(CartItem cartItem);
}
