package com.example.shop.product.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도메인 객체 단위 테스트. 스프링도 DB도 띄우지 않는 순수 자바 테스트라 가장 빠르다.
 * 상태 변경 규칙(stop, 나중에 decreaseStock 등)은 여기서 먼저 확인한다.
 */
class ProductTest {

    @Test
    void 등록하면_판매중이다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 판매중지하면_STOPPED가_된다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);

        product.stop();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.STOPPED);
    }

    @Test
    void 이미_판매중지된_상품을_다시_중지해도_STOPPED() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        product.stop();

        product.stop();   // 예외 없이 그대로

        assertThat(product.getStatus()).isEqualTo(ProductStatus.STOPPED);
    }
}
