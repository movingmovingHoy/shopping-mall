package com.example.shop.product.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.dto.ProductCreateRequest;
import com.example.shop.product.dto.ProductResponse;
import com.example.shop.product.mapper.CategoryMapper;
import com.example.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 상품 등록. 관리자인지는 AdminCheckInterceptor가 이미 확인했으므로 여기서는 검사하지 않는다.
     *
     * 카테고리를 미리 확인하는 이유:
     *   확인하지 않으면 INSERT 때 FK 위반(DataIntegrityViolationException)이 나고,
     *   ApiExceptionHandler의 마지막 Exception 핸들러에 잡혀 500으로 응답된다.
     *   클라이언트가 잘못 보낸 값인데 서버 오류로 보이면 안 되므로 404로 알려 준다.
     *   (회원가입의 중복 이메일과 달리, 카테고리를 지우는 API가 없어서 확인과 INSERT 사이에 사라질 일은 없다)
     */
    @Transactional
    public ProductResponse register(ProductCreateRequest request) {
        if (!categoryMapper.existsById(request.getCategoryId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Product product = Product.create(request.getCategoryId(), request.getName(), request.getPrice(),
                request.getStockQuantity(), request.getDescription());
        productMapper.save(product);

        return ProductResponse.from(product);
    }

    /**
     * 상품 상세 조회. 로그인 없이 누구나 쓸 수 있다. (WebMvcConfig에서 /api/products/** 제외)
     * 판매중지(STOPPED) 상품도 404로 숨기지 않고 status와 함께 그대로 응답한다.
     *   - 과거 주문 내역에서 상품을 눌렀을 때 "없는 상품"이 아니라 "판매중지된 상품"으로 보여 줄 수 있다.
     *   - 판매중지라고 화면에 표시할지는 클라이언트가 status를 보고 정한다.
     *   (주문할 때는 판매중인지 다시 확인해야 한다 → 주문 기능에서 처리)
     */
    /**
     * 판매중지 (관리자). 상품을 지우지 않고 status만 STOPPED로 바꾼다. (설계서: 상품은 물리 삭제하지 않음)
     * 순서: 조회 → 도메인 메서드로 상태 변경 → UPDATE
     *   서비스에서 UPDATE 쿼리에 "STOPPED"를 바로 넣지 않고 product.stop()을 거치는 이유:
     *   "판매중지하면 상태가 어떻게 바뀌는가"라는 규칙을 도메인 객체가 갖게 하려는 것이다.
     * 이미 판매중지된 상품도 에러 없이 그대로 STOPPED로 응답한다.
     */
    @Transactional
    public ProductResponse stop(Long productId) {
        Product product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.stop();
        productMapper.updateStatus(product);

        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }
}
