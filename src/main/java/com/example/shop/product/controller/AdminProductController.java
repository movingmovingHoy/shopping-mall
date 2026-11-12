package com.example.shop.product.controller;

import com.example.shop.product.dto.ProductCreateRequest;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 상품 API (/api/admin/products).
 * 누구나 쓰는 조회 API(/api/products)와 컨트롤러를 나눈 이유:
 *   URL 앞부분이 다르고, 권한 검사도 다르다(관리자 API는 AdminCheckInterceptor가 검사).
 *   한 컨트롤러에 섞어 두면 어떤 메서드가 관리자 전용인지 한눈에 알기 어렵다.
 * 로그인/관리자 검사는 인터셉터가 끝냈으므로 여기서는 @Login도 받지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    /** 상품 등록: 새 리소스를 만들었으므로 201 Created */
    @PostMapping
    public ResponseEntity<ProductResponse> register(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 판매중지. 새로 만든 게 없으니 200.
     * PUT/PATCH로 status를 받지 않고 /stop 이라는 별도 URL로 만든 이유:
     *   "판매중지"라는 의도가 URL에 드러나고, 도메인 메서드 stop()과 1:1로 대응한다.
     *   (주문 취소 POST /api/orders/{id}/cancel 과 같은 방식)
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<ProductResponse> stop(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.stop(productId));
    }
}
