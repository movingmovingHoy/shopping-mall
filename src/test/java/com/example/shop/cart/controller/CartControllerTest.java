package com.example.shop.cart.controller;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.service.CartService;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 장바구니 담기 API의 HTTP 응답 확인. 서비스는 가짜(@MockitoBean)이고, 로그인 인터셉터는 실제로 동작한다. */
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CartService cartService;

    @Test
    void 로그인한_회원이_담으면_201과_담긴_정보() throws Exception {
        given(cartService.add(eq(1L), any()))
                .willReturn(CartItemResponse.from(CartItem.create(1L, 5L, 2)));

        mockMvc.perform(post("/api/cart/items").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "quantity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(5))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "quantity": 2}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));

        verify(cartService, never()).add(any(), any());
    }

    @Test
    void 수량이_0이면_400() throws Exception {
        mockMvc.perform(post("/api/cart/items").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));

        verify(cartService, never()).add(any(), any());
    }

    @Test
    void 상품을_빼먹으면_400() throws Exception {
        mockMvc.perform(post("/api/cart/items").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("productId"));
    }

    @Test
    void 없는_상품이면_404() throws Exception {
        given(cartService.add(any(), any())).willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(post("/api/cart/items").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 999, "quantity": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void 판매중지_상품이면_409() throws Exception {
        given(cartService.add(any(), any())).willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE));

        mockMvc.perform(post("/api/cart/items").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "quantity": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_ON_SALE"));
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE, Role.USER);
        return session;
    }
}
