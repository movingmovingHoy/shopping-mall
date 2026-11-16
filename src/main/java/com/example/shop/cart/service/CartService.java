package com.example.shop.cart.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import com.example.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    /**
     * 장바구니 담기.
     *  1) 상품이 없으면 404, 판매중지(STOPPED)면 거절 (살 수 없는 상품은 담지 못하게)
     *  2) 이미 담은 상품이면 새 줄을 만들지 않고 수량을 더한다.
     *     (member_id, product_id)가 유니크라서 INSERT를 또 하면 DB 오류(500)가 난다.
     *  3) 처음 담는 상품이면 새로 저장한다.
     *
     * 재고는 여기서 검사하지 않는다. 장바구니는 "사고 싶은 목록"이고 재고는 계속 변한다.
     * 재고가 부족한지는 주문할 때 확인한다.
     */
    @Transactional
    public CartItemResponse add(Long memberId, CartItemAddRequest request) {
        Product product = productMapper.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }

        Optional<CartItem> existing = cartItemMapper.findByMemberIdAndProductId(memberId, product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.addQuantity(request.getQuantity());
            cartItemMapper.updateQuantity(item);   // MyBatis는 변경 감지가 없으므로 직접 UPDATE
            return CartItemResponse.from(item);
        }

        CartItem item = CartItem.create(memberId, product.getId(), request.getQuantity());
        cartItemMapper.save(item);
        return CartItemResponse.from(item);
    }
}
