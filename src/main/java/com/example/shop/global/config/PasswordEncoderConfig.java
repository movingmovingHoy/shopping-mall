package com.example.shop.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * spring-security-crypto의 BCryptPasswordEncoder만 빈으로 등록한다.
 * (Spring Security 전체를 쓰는 것이 아니므로 필터나 로그인 화면 같은 것은 생기지 않는다)
 * 반환 타입을 인터페이스(PasswordEncoder)로 두면, 나중에 암호화 방식을 바꿔도 사용하는 쪽 코드는 그대로다.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
