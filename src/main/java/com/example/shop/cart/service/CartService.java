package com.example.shop.cart.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemDetailResponse;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.dto.CartItemUpdateRequest;
import com.example.shop.cart.dto.CartResponse;
import com.example.shop.cart.dto.CartSearchRequest;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import com.example.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    /**
     * 장바구니 조회 (페이징). 쿼리 세 번: 이번 페이지 목록, 전체 개수, 전체 합계 금액.
     * 합계를 이 페이지 목록으로 계산하지 않는 이유: 2페이지를 볼 때 1페이지 금액이 빠진 값이 나온다.
     * 로그인한 회원 본인의 장바구니만 조회한다. memberId는 세션에서 왔으므로 남의 장바구니는 볼 수 없다.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long memberId, CartSearchRequest request) {
        List<CartItemDetailResponse> items = cartItemMapper
                .findAllByMemberId(memberId, request.getSize(), request.getOffset())
                .stream()
                .map(CartItemDetailResponse::from)
                .toList();
        long totalCount = cartItemMapper.countByMemberId(memberId);
        long totalPrice = cartItemMapper.sumOnSalePriceByMemberId(memberId);

        return CartResponse.of(items, request.getPage(), request.getSize(), totalCount, totalPrice);
    }
    /**
     * 수량 변경. 순서: 내 항목 조회 → 도메인 메서드로 변경 → UPDATE
     * 남의 항목이거나 없는 항목이면 둘 다 CART_ITEM_NOT_FOUND(404). 소유자 확인은 조회 SQL(member_id 조건)이 한다.
     * 판매중지 상품도 수량은 바꿀 수 있다. 주문할 때 판매중인지 확인한다.
     */
    @Transactional
    public CartItemResponse updateQuantity(Long memberId, Long cartItemId, CartItemUpdateRequest request) {
        CartItem item = cartItemMapper.findByIdAndMemberId(cartItemId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        item.changeQuantity(request.getQuantity());
        cartItemMapper.updateQuantity(item);

        return CartItemResponse.from(item);
    }

    /**
     * 삭제. 순서: 내 항목 조회 → DELETE
     * 소유자 확인이 먼저인 이유: deleteById는 id만 보고 지우므로, 확인 없이 호출하면 남의 항목도 지워진다.
     * 이미 지워진 항목을 또 지우면 404다. (판매중지처럼 "또 호출해도 200"으로 만들지 않았다. 지울 대상이 없는 요청이므로)
     */
    @Transactional
    public void delete(Long memberId, Long cartItemId) {
        CartItem item = cartItemMapper.findByIdAndMemberId(cartItemId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemMapper.deleteById(item.getId());
    }

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
