package com.example.shop.cart.controller;

import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.dto.CartResponse;
import com.example.shop.cart.dto.CartSearchRequest;
import com.example.shop.cart.service.CartService;
import com.example.shop.global.web.Login;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장바구니는 로그인이 필요하다. WebMvcConfig에서 /api/cart/** 를 제외하지 않았으므로
 * LoginCheckInterceptor가 먼저 401로 막는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    /** 내 장바구니 조회. ?page=&size= 는 @ModelAttribute로 받는다. (상품 목록과 같은 방식) */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@Login Long memberId,
                                                @Valid @ModelAttribute CartSearchRequest request) {
        return ResponseEntity.ok(cartService.getCart(memberId, request));
    }

    /** 담기. 같은 상품을 또 담아 수량이 합쳐진 경우에도 201로 응답한다. (클라이언트는 응답의 quantity로 결과를 본다) */
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> add(@Login Long memberId,
                                                @Valid @RequestBody CartItemAddRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.add(memberId, request));
    }
}
