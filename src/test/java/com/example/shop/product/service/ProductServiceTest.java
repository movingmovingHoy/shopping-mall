package com.example.shop.product.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.domain.ProductStatus;
import com.example.shop.product.dto.ProductCreateRequest;
import com.example.shop.product.dto.ProductPageResponse;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.dto.ProductSearchRequest;
import com.example.shop.product.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** MemberServiceTest와 같이 shop_test DB를 실제로 쓰고, 테스트마다 롤백한다. */
@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    ProductService productService;

    @Autowired
    ProductMapper productMapper;

    @Test
    void 상품_등록_성공() {
        ProductCreateRequest request = new ProductCreateRequest(1L, "반팔 티셔츠", 15000, 10, "면 100%");

        ProductResponse response = productService.register(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ProductStatus.ON_SALE);   // 등록하면 바로 판매중

        Product saved = productMapper.findById(response.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("반팔 티셔츠");
        assertThat(saved.getPrice()).isEqualTo(15000);
        assertThat(saved.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void 없는_카테고리면_CATEGORY_NOT_FOUND() {
        ProductCreateRequest request = new ProductCreateRequest(999_999L, "반팔 티셔츠", 15000, 10, null);

        // FK 위반(DataIntegrityViolationException)이 아니라 BusinessException으로 바뀌어 나와야 한다
        assertThatThrownBy(() -> productService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void 판매중지_성공() {
        Long productId = registerProduct();

        ProductResponse response = productService.stop(productId);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.STOPPED);
        // 응답뿐 아니라 DB에도 반영됐는지 (updateStatus 호출을 빼먹으면 여기서 실패)
        assertThat(productMapper.findById(productId).orElseThrow().getStatus()).isEqualTo(ProductStatus.STOPPED);
    }

    @Test
    void 이미_판매중지된_상품도_에러_없이_STOPPED() {
        Long productId = registerProduct();
        productService.stop(productId);

        ProductResponse response = productService.stop(productId);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.STOPPED);
    }

    @Test
    void 없는_상품을_판매중지하면_PRODUCT_NOT_FOUND() {
        assertThatThrownBy(() -> productService.stop(999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void 상품_목록은_페이지_정보와_전체_개수를_함께_응답한다() {
        registerProduct();
        registerProduct();
        registerProduct();

        ProductSearchRequest request = new ProductSearchRequest();
        request.setPage(1);
        request.setSize(2);

        ProductPageResponse response = productService.getProducts(request);

        assertThat(response.getProducts()).hasSize(2);   // 이번 페이지는 2개
        assertThat(response.getTotalCount()).isEqualTo(3);  // 전체는 3개
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
    }

    @Test
    void 상품이_없으면_빈_목록() {
        ProductPageResponse response = productService.getProducts(new ProductSearchRequest());

        assertThat(response.getProducts()).isEmpty();   // null이 아니라 빈 리스트
        assertThat(response.getTotalCount()).isZero();
    }

    private Long registerProduct() {
        return productService.register(new ProductCreateRequest(1L, "반팔 티셔츠", 15000, 10, null)).getId();
    }

    @Test
    void 상품_상세_조회_성공() {
        Long productId = productService.register(
                new ProductCreateRequest(1L, "반팔 티셔츠", 15000, 10, "면 100%")).getId();

        ProductResponse response = productService.getProduct(productId);

        assertThat(response.getId()).isEqualTo(productId);
        assertThat(response.getName()).isEqualTo("반팔 티셔츠");
        assertThat(response.getDescription()).isEqualTo("면 100%");
        assertThat(response.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 없는_상품이면_PRODUCT_NOT_FOUND() {
        assertThatThrownBy(() -> productService.getProduct(999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
