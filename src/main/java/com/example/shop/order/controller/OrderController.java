package com.example.shop.order.controller;

import com.example.shop.global.web.Login;
import com.example.shop.order.dto.CartOrderCreateRequest;
import com.example.shop.order.dto.OrderCreateRequest;
import com.example.shop.order.dto.OrderDetailResponse;
import com.example.shop.order.dto.OrderPageResponse;
import com.example.shop.order.dto.OrderSearchRequest;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문은 로그인이 필요하다. WebMvcConfig에서 /api/orders 를 제외하지 않았으므로
 * LoginCheckInterceptor가 먼저 401로 막는다.
 * 생성 API는 새 주문이 만들어지므로 201, 조회 API는 200.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /** 바로 구매. 요청에 적힌 상품만 주문한다. 장바구니는 건드리지 않는다 */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Login Long memberId,
                                                @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(memberId, request));
    }

    /** 장바구니 주문. 내 장바구니 전체를 주문하고 장바구니를 비운다 */
    @PostMapping("/cart")
    public ResponseEntity<OrderResponse> createFromCart(@Login Long memberId,
                                                        @Valid @RequestBody CartOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createFromCart(memberId, request));
    }

    /** 내 주문 목록. 쿼리 파라미터(?page=1&size=20)는 @ModelAttribute로 받는다 */
    @GetMapping
    public ResponseEntity<OrderPageResponse> getOrders(@Login Long memberId,
                                                       @Valid @ModelAttribute OrderSearchRequest request) {
        return ResponseEntity.ok(orderService.getOrders(memberId, request));
    }

    /** 내 주문 상세. 남의 주문 id를 넣으면 404 (서비스가 판단한다) */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrder(@Login Long memberId, @PathVariable("id") Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(memberId, orderId));
    }
}
