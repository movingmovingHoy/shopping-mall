package com.example.shop.member.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.domain.Role;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.dto.MemberSignupRequest;
import com.example.shop.member.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 스프링을 띄우고 shop_test DB를 실제로 사용한다.
 * 테스트에 붙은 @Transactional은 테스트가 끝나면 롤백하므로 데이터가 남지 않는다.
 */
@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberMapper memberMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 회원가입_성공() {
        MemberSignupRequest request = new MemberSignupRequest("user@test.com", "password123", "홍길동");

        MemberResponse response = memberService.signup(request);

        Member saved = memberMapper.findById(response.getId()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void 비밀번호는_암호화되어_저장된다() {
        MemberSignupRequest request = new MemberSignupRequest("user@test.com", "password123", "홍길동");

        MemberResponse response = memberService.signup(request);

        Member saved = memberMapper.findById(response.getId()).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("password123");               // 평문이 아님
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue(); // 원래 비밀번호와 일치
    }

    @Test
    void 이미_가입된_이메일이면_예외() {
        memberService.signup(new MemberSignupRequest("user@test.com", "password123", "홍길동"));
        MemberSignupRequest duplicate = new MemberSignupRequest("user@test.com", "other1234", "김철수");

        assertThatThrownBy(() -> memberService.signup(duplicate))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

}
