package com.example.shop.product.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
