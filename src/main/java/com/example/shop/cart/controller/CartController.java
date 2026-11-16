package com.example.shop.cart.controller;

import com.example.shop.cart.dto.CartItemAddRequest;
import com.example.shop.cart.dto.CartItemResponse;
import com.example.shop.cart.dto.CartItemUpdateRequest;
import com.example.shop.cart.dto.CartResponse;
import com.example.shop.cart.dto.CartSearchRequest;
import com.example.shop.cart.service.CartService;
import com.example.shop.global.web.Login;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * 수량 변경. PATCH = 일부만 수정 (여기서는 quantity 하나). 경로의 {id}는 장바구니 항목 id다. (상품 id가 아니다)
     * 내 항목인지는 서비스가 확인한다. 응답은 새로 만든 게 없으니 200.
     */
    @PatchMapping("/items/{id}")
    public ResponseEntity<CartItemResponse> updateQuantity(@Login Long memberId,
                                                           @PathVariable("id") Long cartItemId,
                                                           @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(memberId, cartItemId, request));
    }

    /** 삭제. 성공하면 돌려줄 내용이 없으므로 본문 없이 204 No Content. */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> delete(@Login Long memberId, @PathVariable("id") Long cartItemId) {
        cartService.delete(memberId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    /** 담기. 같은 상품을 또 담아 수량이 합쳐진 경우에도 201로 응답한다. (클라이언트는 응답의 quantity로 결과를 본다) */
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> add(@Login Long memberId,
                                                @Valid @RequestBody CartItemAddRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.add(memberId, request));
    }
}
