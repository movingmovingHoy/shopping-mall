package com.example.shop.product.controller;

import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 누구나 쓰는 상품 조회 API (/api/products). 관리자용 등록/수정은 AdminProductController에 있다.
 * 다음에 만들 상품 목록(GET /api/products)도 여기에 추가한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 상세. URL의 {id}가 productId 파라미터로 들어온다.
     * /api/products/abc 처럼 숫자가 아니면 Long으로 바꾸지 못해 MethodArgumentTypeMismatchException이 나고,
     * ApiExceptionHandler가 이미 400(INVALID_INPUT)으로 처리한다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
