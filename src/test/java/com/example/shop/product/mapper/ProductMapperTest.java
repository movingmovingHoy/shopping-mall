package com.example.shop.product.mapper;

import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MemberMapperTest와 같은 설정. 카테고리는 data.sql이 넣어 둔 id 1(의류)을 쓴다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductMapperTest {

    static final Long CATEGORY_ID = 1L;   // data.sql의 '의류'

    @Autowired
    ProductMapper productMapper;

    @Test
    void 저장하면_id가_채워지고_조회할_수_있다() {
        Product product = Product.create(CATEGORY_ID, "반팔 티셔츠", 15000, 10, "면 100%");

        productMapper.save(product);

        assertThat(product.getId()).isNotNull();
        Product found = productMapper.findById(product.getId()).orElseThrow();
        assertThat(found.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(found.getName()).isEqualTo("반팔 티셔츠");
        assertThat(found.getPrice()).isEqualTo(15000);
        assertThat(found.getStockQuantity()).isEqualTo(10);           // stock_quantity → stockQuantity 매핑 확인
        assertThat(found.getStatus()).isEqualTo(ProductStatus.ON_SALE); // 문자열 → enum 매핑 확인
        assertThat(found.getDescription()).isEqualTo("면 100%");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 설명_없이도_저장된다() {
        Product product = Product.create(CATEGORY_ID, "반팔 티셔츠", 15000, 10, null);

        productMapper.save(product);

        assertThat(productMapper.findById(product.getId()).orElseThrow().getDescription()).isNull();
    }

    @Test
    void 없는_id로_조회하면_빈_Optional() {
        assertThat(productMapper.findById(999_999L)).isEmpty();
    }

    /**
     * ProductService가 카테고리를 미리 확인하는 이유를 실제 DB로 보여 준다.
     * 확인 없이 저장하면 FK 위반 예외가 나고, 이 예외는 BusinessException이 아니라서 500으로 응답된다.
     */
    @Test
    void 없는_카테고리로_저장하면_FK_위반() {
        Product product = Product.create(999_999L, "반팔 티셔츠", 15000, 10, null);

        assertThatThrownBy(() -> productMapper.save(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
