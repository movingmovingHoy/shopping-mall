package com.example.shop.order.controller;

import com.example.shop.global.web.Login;
import com.example.shop.order.dto.OrderCreateRequest;
import com.example.shop.order.dto.OrderResponse;
import com.example.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문은 로그인이 필요하다. WebMvcConfig에서 /api/orders 를 제외하지 않았으므로
 * LoginCheckInterceptor가 먼저 401로 막는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /** 주문 생성. 새 주문이 만들어졌으므로 201. items가 있으면 단건 주문, 없으면 장바구니 전체 주문 (OrderCreateRequest 참고) */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Login Long memberId,
                                                @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(memberId, request));
    }
}
