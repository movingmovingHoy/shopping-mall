package com.example.shop.cart.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 다른 서비스 테스트처럼 shop_test DB를 실제로 쓰고 테스트마다 롤백한다. */
@SpringBootTest
@Transactional
class CartServiceTest {

    @Autowired
    CartService cartService;
    @Autowired
    CartItemMapper cartItemMapper;
    @Autowired
    MemberMapper memberMapper;
    @Autowired
    ProductMapper productMapper;

    Member member;
    Product product;

    @BeforeEach
    void setUp() {
        member = Member.create("cart@test.com", "encoded", "홍길동");
        memberMapper.save(member);
        product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        productMapper.save(product);
    }

    @Test
    void 처음_담으면_새_줄이_생긴다() {
        CartItemResponse response = cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 2));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(2);
        CartItem saved = cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId()).orElseThrow();
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void 같은_상품을_또_담으면_수량이_합쳐지고_줄은_늘지_않는다() {
        CartItemResponse first = cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 2));

        CartItemResponse second = cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 3));

        assertThat(second.getId()).isEqualTo(first.getId());   // 같은 줄
        assertThat(second.getQuantity()).isEqualTo(5);
        assertThat(cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId())
                .orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void 없는_상품이면_PRODUCT_NOT_FOUND() {
        assertThatThrownBy(() -> cartService.add(member.getId(), new CartItemAddRequest(999_999L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void 판매중지_상품이면_PRODUCT_NOT_ON_SALE이고_담기지_않는다() {
        product.stop();
        productMapper.updateStatus(product);

        assertThatThrownBy(() -> cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE);
        assertThat(cartItemMapper.findByMemberIdAndProductId(member.getId(), product.getId())).isEmpty();
    }
}
