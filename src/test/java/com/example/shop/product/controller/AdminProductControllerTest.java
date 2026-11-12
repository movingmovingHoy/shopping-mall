package com.example.shop.product.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Role;
import com.example.shop.product.domain.Product;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 상품 등록 API의 HTTP 응답을 확인한다. 서비스는 가짜(@MockitoBean)다.
 * WebMvcConfig가 함께 뜨므로 관리자 인터셉터도 실제처럼 동작한다 → 요청마다 관리자 세션을 넣어 준다.
 */
@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {

    static final String VALID_BODY = """
            {"categoryId": 1, "name": "반팔 티셔츠", "price": 15000, "stockQuantity": 10, "description": "면 100%"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    @Test
    void 관리자가_등록하면_201과_상품정보를_응답한다() throws Exception {
        ProductResponse response = ProductResponse.from(Product.create(1L, "반팔 티셔츠", 15000, 10, "면 100%"));
        given(productService.register(any())).willReturn(response);

        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("반팔 티셔츠"))
                .andExpect(jsonPath("$.price").value(15000))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void 가격이_음수면_400() throws Exception {
        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "name": "반팔 티셔츠", "price": -1, "stockQuantity": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("price"));

        // 응답만으로는 "서비스까지 갔다가 실패했는지"를 알 수 없어서 verify로 확인한다
        verify(productService, never()).register(any());
    }

    @Test
    void 필수값을_빼면_400() throws Exception {
        // price를 아예 안 보냄 → Integer라서 null → @NotNull에 걸린다 (int였다면 0이 들어가 통과했을 것)
        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "name": "반팔 티셔츠", "stockQuantity": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"))
                .andExpect(jsonPath("$.errors[0].message").value("가격은 필수입니다."));
    }

    @Test
    void 없는_카테고리면_404() throws Exception {
        given(productService.register(any())).willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void 일반_회원이면_403이고_등록되지_않는다() throws Exception {
        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"));

        verify(productService, never()).register(any());   // 인터셉터에서 막혀 서비스까지 가지 않았는지
    }

    private MockHttpSession loginSession(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE, role);
        return session;
    }
}
