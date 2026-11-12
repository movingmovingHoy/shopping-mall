package com.example.shop.product.mapper;

import com.example.shop.product.domain.Product;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/** SQL은 resources/mapper/product/ProductMapper.xml에 있다. */
@Mapper
public interface ProductMapper {

    /** 저장 후 DB가 만든 id가 product.id에 채워진다 (useGeneratedKeys) */
    void save(Product product);

    /** 지금은 테스트에서 저장 결과를 확인하는 데 쓰고, 상품 상세 API에서도 쓸 예정이다 */
    Optional<Product> findById(Long id);

    /** product.status 값으로 DB를 바꾼다. 도메인 메서드(stop 등)로 상태를 바꾼 뒤 호출한다 */
    void updateStatus(Product product);
}
