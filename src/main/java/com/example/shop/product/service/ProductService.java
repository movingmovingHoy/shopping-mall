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
}
