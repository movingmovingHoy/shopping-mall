package com.example.shop.order.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Role;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.dto.OrderItemResponse;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 주문 생성 API(바로 구매, 장바구니 주문)의 HTTP 응답 확인. 서비스는 가짜(@MockitoBean)이고, 로그인 인터셉터는 실제로 동작한다. */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    void 주문하면_201과_주문_정보() throws Exception {
        Order order = Order.create(1L, 30000, "홍길동", "서울시");
        OrderItem item = OrderItem.create(null, 5L, 15000, 2);
        given(orderService.create(eq(1L), any()))
                .willReturn(OrderResponse.of(order, List.of(OrderItemResponse.of(item, "반팔 티셔츠"))));

        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시",
                                 "items": [{"productId": 5, "quantity": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(30000))
                .andExpect(jsonPath("$.items[0].productName").value("반팔 티셔츠"))
                .andExpect(jsonPath("$.items[0].orderPrice").value(15000));
    }

    @Test
    void 바로_구매에_items가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void 바로_구매에_items가_빈_배열이면_400_장바구니_주문으로_바뀌지_않는다() throws Exception {
        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시", "items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).create(any(), any());
        verify(orderService, never()).createFromCart(any(), any());
    }

    // ===== 장바구니 주문 (POST /api/orders/cart) =====

    @Test
    void 장바구니_주문하면_201() throws Exception {
        Order order = Order.create(1L, 15000, "홍길동", "서울시");
        OrderItem item = OrderItem.create(null, 5L, 15000, 1);
        given(orderService.createFromCart(eq(1L), any()))
                .willReturn(OrderResponse.of(order, List.of(OrderItemResponse.of(item, "반팔 티셔츠"))));

        mockMvc.perform(post("/api/orders/cart").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(15000))
                .andExpect(jsonPath("$.items[0].productName").value("반팔 티셔츠"));
    }

    @Test
    void 장바구니_주문도_로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/orders/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));

        verify(orderService, never()).createFromCart(any(), any());
    }

    @Test
    void 장바구니_주문에_받는_사람이_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/orders/cart").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": " ", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).createFromCart(any(), any());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void 받는_사람이_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": " ", "receiverAddress": "서울시",
                                 "items": [{"productId": 5, "quantity": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void 주소가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "items": [{"productId": 5, "quantity": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void items_안의_수량이_0이면_400() throws Exception {
        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시",
                                 "items": [{"productId": 5, "quantity": 0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).create(any(), any());
    }

    @Test
    void 재고가_부족하면_409() throws Exception {
        given(orderService.create(any(), any())).willThrow(new BusinessException(ErrorCode.OUT_OF_STOCK));

        mockMvc.perform(post("/api/orders").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시",
                                 "items": [{"productId": 5, "quantity": 100}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    void 장바구니가_비어_있으면_400() throws Exception {
        given(orderService.createFromCart(any(), any())).willThrow(new BusinessException(ErrorCode.EMPTY_CART));

        mockMvc.perform(post("/api/orders/cart").session(loginSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "홍길동", "receiverAddress": "서울시"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE, Role.USER);
        return session;
    }
}
