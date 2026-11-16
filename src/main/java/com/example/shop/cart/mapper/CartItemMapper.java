package com.example.shop.cart.mapper;

import com.example.shop.cart.domain.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/** SQL은 resources/mapper/cart/CartItemMapper.xml에 있다. */
@Mapper
public interface CartItemMapper {

    /** 저장 후 DB가 만든 id가 cartItem.id에 채워진다 (useGeneratedKeys) */
    void save(CartItem cartItem);

    /** 이 회원의 장바구니에 이 상품이 이미 있는지 찾는다 */
    Optional<CartItem> findByMemberIdAndProductId(@Param("memberId") Long memberId,
                                                  @Param("productId") Long productId);

    /** cartItem.quantity 값으로 DB 수량을 바꾼다. 도메인 메서드(addQuantity)로 바꾼 뒤 호출한다 */
    void updateQuantity(CartItem cartItem);
}
