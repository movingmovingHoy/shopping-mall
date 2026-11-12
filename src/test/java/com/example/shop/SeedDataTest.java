package com.example.shop;

import com.example.shop.member.domain.Role;
import com.example.shop.member.dto.LoginRequest;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.service.MemberService;
import com.example.shop.product.mapper.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * data.sql이 제대로 들어갔는지 확인한다.
 * 특히 관리자 비밀번호는 BCrypt 해시를 손으로 붙여 넣었기 때문에,
 * 해시가 틀렸으면 Postman에서 로그인해 보기 전까지 모른다 → 테스트로 미리 잡는다.
 */
@SpringBootTest
class SeedDataTest {

    @Autowired
    MemberService memberService;

    @Autowired
    CategoryMapper categoryMapper;

    @Test
    void 시드_관리자_계정으로_로그인할_수_있다() {
        MemberResponse admin = memberService.login(new LoginRequest("admin@shop.com", "admin1234"));

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        // spring.sql.init.encoding이 빠지면 Windows에서 data.sql을 MS949로 읽어 한글이 깨진다
        assertThat(admin.getName()).isEqualTo("관리자");
    }

    @Test
    void 시드_카테고리가_있다() {
        assertThat(categoryMapper.existsById(1L)).isTrue();
        assertThat(categoryMapper.existsById(2L)).isTrue();
        assertThat(categoryMapper.existsById(3L)).isTrue();
    }
}
