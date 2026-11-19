package com.example.shop.order.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Role;
import com.example.shop.order.domain.Order;
import com.example.shop.order.domain.OrderItem;
import com.example.shop.order.domain.OrderStatus;
import com.example.shop.order.dto.OrderDetail;
import com.example.shop.order.dto.OrderDetailResponse;
import com.example.shop.order.dto.OrderItemDetail;
import com.example.shop.order.dto.OrderItemResponse;
import com.example.shop.order.dto.OrderPageResponse;
import com.example.shop.order.dto.OrderSearchRequest;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    // ===== 주문 목록/상세 =====

    @Test
    void 주문_목록은_200이고_쿼리_파라미터가_전달된다() throws Exception {
        given(orderService.getOrders(eq(1L), any())).willReturn(OrderPageResponse.of(List.of(), 2, 5, 7));

        mockMvc.perform(get("/api/orders").param("page", "2").param("size", "5").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalCount").value(7));

        // @ModelAttribute + @Setter로 값이 실제로 들어갔는지 (setter가 없으면 기본값 1, 20이 된다)
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        verify(orderService).getOrders(eq(1L), captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
    }

    @Test
    void 주문_목록_size가_100을_넘으면_400() throws Exception {
        mockMvc.perform(get("/api/orders").param("size", "101").session(loginSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(orderService, never()).getOrders(any(), any());
    }

    @Test
    void 로그인하지_않으면_주문_목록은_401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }

    @Test
    void 주문_상세는_200과_주문상품() throws Exception {
        OrderDetailResponse detail = sampleDetail();   // given(...) 안에서 만들면 목 설정이 겹쳐 UnfinishedStubbingException
        given(orderService.getOrder(1L, 10L)).willReturn(detail);

        mockMvc.perform(get("/api/orders/10").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.orderedAt").exists())
                .andExpect(jsonPath("$.items[0].productName").value("반팔 티셔츠"))
                .andExpect(jsonPath("$.items[0].orderPrice").value(15000));
    }

    @Test
    void 없거나_남의_주문_상세는_404() throws Exception {
        given(orderService.getOrder(1L, 99L)).willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/orders/99").session(loginSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void 주문_id가_숫자가_아니면_400() throws Exception {
        mockMvc.perform(get("/api/orders/abc").session(loginSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    /**
     * OrderDetail은 MyBatis만 만드는 조회 객체라(생성자가 private) 테스트에서는 Mockito로 가짜를 만든다.
     * 응답 변환(OrderDetailResponse.from)은 진짜 코드가 돈다.
     */
    private OrderDetailResponse sampleDetail() {
        OrderItemDetail item = mock(OrderItemDetail.class);
        given(item.getProductId()).willReturn(5L);
        given(item.getProductName()).willReturn("반팔 티셔츠");
        given(item.getOrderPrice()).willReturn(15000);
        given(item.getQuantity()).willReturn(2);

        OrderDetail detail = mock(OrderDetail.class);
        given(detail.getId()).willReturn(10L);
        given(detail.getStatus()).willReturn(OrderStatus.PAID);
        given(detail.getTotalPrice()).willReturn(30000);
        given(detail.getReceiverName()).willReturn("홍길동");
        given(detail.getReceiverAddress()).willReturn("서울시");
        given(detail.getOrderedAt()).willReturn(LocalDateTime.of(2026, 11, 19, 12, 0));
        given(detail.getItems()).willReturn(List.of(item));
        return OrderDetailResponse.from(detail);
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE, Role.USER);
        return session;
    }
}
