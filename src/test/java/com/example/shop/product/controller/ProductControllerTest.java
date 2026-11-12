package com.example.shop.product.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.dto.ProductPageResponse;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.dto.ProductSearchRequest;
import com.example.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 상품 조회 API. 요청에 세션을 넣지 않는다 → 로그인 없이도 되는지가 확인된다.
 * (WebMvcConfig가 함께 뜨므로, /api/products/** 제외 설정이 빠지면 401이 나서 테스트가 실패한다)
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    // ===== 상품 목록 =====

    @Test
    void 로그인_없이_상품_목록을_조회한다() throws Exception {
        ProductResponse item = ProductResponse.from(Product.create(1L, "반팔 티셔츠", 15000, 10, null));
        given(productService.getProducts(any())).willReturn(ProductPageResponse.of(List.of(item), 1, 20, 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].name").value("반팔 티셔츠"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    /**
     * 쿼리 파라미터가 DTO에 제대로 들어갔는지 확인한다.
     * 응답은 가짜 서비스가 만든 것이라 응답만 봐서는 알 수 없다 → 서비스에 넘어간 인자를 직접 꺼내 본다.
     * ArgumentCaptor: Mock이 호출될 때 받은 인자를 붙잡아 두는 도구.
     * ProductSearchRequest에서 @Setter를 빼면 page/size가 기본값(1, 20)으로 남아서 이 테스트가 실패한다.
     */
    @Test
    void 쿼리_파라미터가_검색_조건으로_들어간다() throws Exception {
        given(productService.getProducts(any())).willReturn(ProductPageResponse.of(List.of(), 2, 10, 0));

        mockMvc.perform(get("/api/products").param("categoryId", "1").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchRequest> captor = ArgumentCaptor.forClass(ProductSearchRequest.class);
        verify(productService).getProducts(captor.capture());
        ProductSearchRequest request = captor.getValue();
        assertThat(request.getCategoryId()).isEqualTo(1L);
        assertThat(request.getPage()).isEqualTo(2);
        assertThat(request.getSize()).isEqualTo(10);
        assertThat(request.getOffset()).isEqualTo(10);   // (2 - 1) * 10
    }

    @Test
    void 파라미터가_없으면_기본값_page1_size20() throws Exception {
        given(productService.getProducts(any())).willReturn(ProductPageResponse.of(List.of(), 1, 20, 0));

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());

        ArgumentCaptor<ProductSearchRequest> captor = ArgumentCaptor.forClass(ProductSearchRequest.class);
        verify(productService).getProducts(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isNull();
        assertThat(captor.getValue().getPage()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    void page가_0이면_400() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        verify(productService, never()).getProducts(any());
    }

    @Test
    void size가_100을_넘으면_400() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").value("페이지 크기는 100 이하여야 합니다."));
    }

    // ===== 상품 상세 =====

    @Test
    void 로그인_없이_상품_상세를_조회한다() throws Exception {
        ProductResponse response = ProductResponse.from(Product.create(1L, "반팔 티셔츠", 15000, 10, "면 100%"));
        given(productService.getProduct(1L)).willReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("반팔 티셔츠"))
                .andExpect(jsonPath("$.price").value(15000))
                .andExpect(jsonPath("$.description").value("면 100%"))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void 없는_상품이면_404() throws Exception {
        given(productService.getProduct(999L)).willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    void id가_숫자가_아니면_400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        // "abc"를 Long으로 바꾸는 단계에서 막혀 서비스까지 가지 않는다
        verify(productService, never()).getProduct(any());
    }
}
