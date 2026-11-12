package com.example.shop.product.mapper;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryMapperTest {

    @Autowired
    CategoryMapper categoryMapper;

    @Test
    void 카테고리_존재_여부를_확인한다() {
        assertThat(categoryMapper.existsById(1L)).isTrue();         // data.sql의 '의류'
        assertThat(categoryMapper.existsById(999_999L)).isFalse();
    }
}
