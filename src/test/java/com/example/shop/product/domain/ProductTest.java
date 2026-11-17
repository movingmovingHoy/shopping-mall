package com.example.shop.product.domain;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void 수정하면_정보가_바뀌고_상태는_그대로다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, "면 100%");
        product.stop();

        product.update(2L, "긴팔 티셔츠", 20000, 5, null);

        assertThat(product.getCategoryId()).isEqualTo(2L);
        assertThat(product.getName()).isEqualTo("긴팔 티셔츠");
        assertThat(product.getPrice()).isEqualTo(20000);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(product.getDescription()).isNull();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.STOPPED);   // 수정해도 판매 재개되지 않는다
    }

    @Test
    void 이미_판매중지된_상품을_다시_중지해도_STOPPED() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);
        product.stop();

        product.stop();   // 예외 없이 그대로

        assertThat(product.getStatus()).isEqualTo(ProductStatus.STOPPED);
    }

    @Test
    void 재고를_차감하면_그만큼_줄어든다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);

        product.decreaseStock(3);

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void 재고와_같은_수량까지는_차감할_수_있다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);

        product.decreaseStock(10);   // 경계값: 정확히 0이 되는 것은 허용

        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    void 재고보다_많이_차감하면_OUT_OF_STOCK이고_재고는_그대로다() {
        Product product = Product.create(1L, "반팔 티셔츠", 15000, 10, null);

        assertThatThrownBy(() -> product.decreaseStock(11))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OUT_OF_STOCK);
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }
}
