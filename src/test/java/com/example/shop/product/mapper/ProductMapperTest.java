package com.example.shop.product.mapper;

import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

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
    void 상태를_바꾸면_DB에_반영되고_다른_컬럼은_그대로다() {
        Product product = Product.create(CATEGORY_ID, "반팔 티셔츠", 15000, 10, "면 100%");
        productMapper.save(product);

        product.stop();                        // 객체만 바뀐 상태 (DB는 아직 ON_SALE)
        productMapper.updateStatus(product);   // MyBatis는 직접 UPDATE해야 DB에 반영된다

        Product found = productMapper.findById(product.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ProductStatus.STOPPED);
        assertThat(found.getPrice()).isEqualTo(15000);       // status만 바꾸는 쿼리인지 확인
        assertThat(found.getStockQuantity()).isEqualTo(10);
    }

    // ===== 목록 조회 (findAll / countAll) =====
    // 테스트마다 롤백되고 data.sql에는 상품이 없으므로, 각 테스트가 넣은 상품만 DB에 있다.

    @Test
    void 페이지_크기만큼_자르고_OFFSET만큼_건너뛴다() {
        saveProduct(CATEGORY_ID, "상품1");
        saveProduct(CATEGORY_ID, "상품2");
        saveProduct(CATEGORY_ID, "상품3");

        assertThat(productMapper.findAll(null, 2, 0)).hasSize(2);   // 1페이지: 2개
        assertThat(productMapper.findAll(null, 2, 2)).hasSize(1);   // 2페이지: 남은 1개
        assertThat(productMapper.findAll(null, 2, 4)).isEmpty();    // 3페이지: 없음
    }

    @Test
    void 최신순으로_정렬된다() {
        saveProduct(CATEGORY_ID, "먼저 등록");
        saveProduct(CATEGORY_ID, "나중에 등록");

        List<Product> products = productMapper.findAll(null, 10, 0);

        assertThat(products).extracting(Product::getName)
                .containsExactly("나중에 등록", "먼저 등록");   // 순서까지 확인
    }

    @Test
    void 카테고리를_지정하면_그_카테고리만_조회한다() {
        saveProduct(1L, "티셔츠");     // 의류
        saveProduct(2L, "노트북");     // 전자기기

        assertThat(productMapper.findAll(1L, 10, 0)).extracting(Product::getName).containsExactly("티셔츠");
        assertThat(productMapper.findAll(null, 10, 0)).hasSize(2);   // null이면 <if>가 빠져서 전체
    }

    @Test
    void 판매중지_상품은_목록과_개수에서_빠진다() {
        saveProduct(CATEGORY_ID, "판매중");
        Product stopped = saveProduct(CATEGORY_ID, "판매중지");
        stopped.stop();
        productMapper.updateStatus(stopped);

        assertThat(productMapper.findAll(null, 10, 0)).extracting(Product::getName).containsExactly("판매중");
        assertThat(productMapper.countAll(null)).isEqualTo(1);
    }

    @Test
    void 개수는_LIMIT과_상관없이_조건에_맞는_전체를_센다() {
        saveProduct(1L, "티셔츠1");
        saveProduct(1L, "티셔츠2");
        saveProduct(2L, "노트북");

        assertThat(productMapper.countAll(null)).isEqualTo(3);
        assertThat(productMapper.countAll(1L)).isEqualTo(2);   // findAll과 같은 <include> 조건을 쓰는지
    }

    private Product saveProduct(Long categoryId, String name) {
        Product product = Product.create(categoryId, name, 10000, 10, null);
        productMapper.save(product);
        return product;
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
