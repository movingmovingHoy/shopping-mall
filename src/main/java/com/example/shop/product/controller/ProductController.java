package com.example.shop.product.controller;

import com.example.shop.product.dto.ProductPageResponse;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.dto.ProductSearchRequest;
import com.example.shop.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 누구나 쓰는 상품 조회 API (/api/products). 관리자용 등록/판매중지는 AdminProductController에 있다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록. GET /api/products?categoryId=1&page=2&size=10
     *
     * 값을 받는 애노테이션 3가지:
     *   - @PathVariable  : URL 경로 안의 값       /api/products/{id}   (상세 조회)
     *   - @ModelAttribute: 쿼리 파라미터 → 객체    /api/products?page=2 (여기)
     *   - @RequestBody   : JSON 본문 → 객체        POST 요청의 body     (상품 등록)
     * @Valid: @Min/@Max 검증. 실패하면 MethodArgumentNotValidException → ApiExceptionHandler가 400으로 응답한다.
     *   (@RequestBody 검증 실패와 같은 예외라서 핸들러를 새로 만들 필요가 없다)
     */
    @GetMapping
    public ResponseEntity<ProductPageResponse> getProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ResponseEntity.ok(productService.getProducts(request));
    }

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
