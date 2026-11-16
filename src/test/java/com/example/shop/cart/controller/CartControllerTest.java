package com.example.shop.cart.controller;

import com.example.shop.cart.domain.CartItem;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.dto.CartResponse;
import com.example.shop.cart.dto.CartSearchRequest;
import com.example.shop.cart.service.CartService;
import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 장바구니 담기 API의 HTTP 응답 확인. 서비스는 가짜(@MockitoBean)이고, 로그인 인터셉터는 실제로 동작한다. */
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CartService cartService;

    // ===== 삭제 =====

    @Test
    void 삭제하면_204와_빈_본문() throws Exception {
        mockMvc.perform(delete("/api/cart/items/10").session(loginSession()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).delete(1L, 10L);
    }

    @Test
    void 삭제할_때_남의_항목이거나_없으면_404() throws Exception {
        willThrow(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)).given(cartService).delete(any(), any());

        mockMvc.perform(delete("/api/cart/items/999").session(loginSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void 삭제할_때_로그인하지_않으면_401() throws Exception {
        mockMvc.perform(delete("/api/cart/items/10"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).delete(any(), any());
    }

    // ===== 수량 변경 =====

    @Test
    void 수량을_변경하면_200과_바뀐_정보() throws Exception {
        given(cartService.updateQuantity(eq(1L), eq(10L), any()))
                .willReturn(CartItemResponse.from(CartItem.create(1L, 5L, 3)));

        mockMvc.perform(patch("/api/cart/items/10").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void 수량을_0으로_변경하면_400() throws Exception {
        mockMvc.perform(patch("/api/cart/items/10").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));

        verify(cartService, never()).updateQuantity(any(), any(), any());
    }

    @Test
    void 남의_항목이거나_없으면_404() throws Exception {
        given(cartService.updateQuantity(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        mockMvc.perform(patch("/api/cart/items/999").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void 수량_변경할_때_로그인하지_않으면_401() throws Exception {
        mockMvc.perform(patch("/api/cart/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 1}
                                """))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).updateQuantity(any(), any(), any());
    }

    // ===== 장바구니 조회 =====

    @Test
    void 조회하면_200과_목록_합계() throws Exception {
        given(cartService.getCart(eq(1L), any())).willReturn(
                CartResponse.of(List.of(), 1, 20, 0, 0));

        mockMvc.perform(get("/api/cart").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void 쿼리_파라미터_page와_size가_서비스까지_전달된다() throws Exception {
        given(cartService.getCart(any(), any())).willReturn(CartResponse.of(List.of(), 2, 5, 0, 0));

        mockMvc.perform(get("/api/cart?page=2&size=5").session(loginSession()))
                .andExpect(status().isOk());

        ArgumentCaptor<CartSearchRequest> captor = ArgumentCaptor.forClass(CartSearchRequest.class);
        verify(cartService).getCart(eq(1L), captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);   // setter가 없으면 조용히 1로 남는다
        assertThat(captor.getValue().getSize()).isEqualTo(5);
    }

    @Test
    void 조회할_때_로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));

        verify(cartService, never()).getCart(any(), any());
    }

    @Test
    void size가_0이면_400() throws Exception {
        mockMvc.perform(get("/api/cart?size=0").session(loginSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));

        verify(cartService, never()).getCart(any(), any());
    }

    // ===== 장바구니 담기 =====

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
