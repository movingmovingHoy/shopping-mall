package com.example.shop.cart.service;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.dto.CartResponse;
import com.example.shop.cart.dto.CartSearchRequest;
import com.example.shop.cart.mapper.CartItemMapper;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.mapper.MemberMapper;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
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

    // ===== 장바구니 조회 =====

    @Test
    void 조회하면_목록_개수_합계가_나온다() {
        Product pants = Product.create(1L, "청바지", 30000, 5, null);
        productMapper.save(pants);
        cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 2));   // 15000 x 2
        cartService.add(member.getId(), new CartItemAddRequest(pants.getId(), 1));     // 30000 x 1

        CartResponse response = cartService.getCart(member.getId(), new CartSearchRequest());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("청바지");
        assertThat(response.getItems().get(1).getLinePrice()).isEqualTo(30000);   // 15000 x 2
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualTo(60000);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
    }

    @Test
    void 합계는_페이지와_상관없이_전체_기준이다() {
        Product pants = Product.create(1L, "청바지", 30000, 5, null);
        productMapper.save(pants);
        cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 1));
        cartService.add(member.getId(), new CartItemAddRequest(pants.getId(), 1));
        CartSearchRequest request = new CartSearchRequest();
        request.setSize(1);

        CartResponse response = cartService.getCart(member.getId(), request);

        assertThat(response.getItems()).hasSize(1);          // 한 페이지에는 1개
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualTo(45000);   // 합계는 두 상품 전체
    }

    @Test
    void 판매중지된_상품은_목록에_보이지만_합계에서는_빠진다() {
        cartService.add(member.getId(), new CartItemAddRequest(product.getId(), 2));
        product.stop();
        productMapper.updateStatus(product);

        CartResponse response = cartService.getCart(member.getId(), new CartSearchRequest());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductStatus()).isEqualTo(ProductStatus.STOPPED);
        assertThat(response.getTotalPrice()).isZero();
    }

    @Test
    void 다른_회원의_장바구니는_조회되지_않는다() {
        Member other = Member.create("other@test.com", "encoded", "김철수");
        memberMapper.save(other);
        cartService.add(other.getId(), new CartItemAddRequest(product.getId(), 3));

        CartResponse response = cartService.getCart(member.getId(), new CartSearchRequest());

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getTotalPrice()).isZero();
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
