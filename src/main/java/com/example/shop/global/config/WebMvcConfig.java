package com.example.shop.global.config;

import com.example.shop.global.web.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 인터셉터 등록. (다음에 만들 AdminCheckInterceptor, @Login ArgumentResolver도 여기에 등록한다)
 *
 * "로그인 필요한 URL을 나열"하지 않고 "/api/** 전체에 걸고, 로그인 없이 쓰는 URL만 제외"한다.
 * 새 API를 만들 때 등록을 깜빡해도 기본값이 '막힘'이라 안전하다. (반대로 하면 깜빡한 API가 그대로 열린다)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // LoginCheckInterceptor는 필요한 의존성이 없어서 빈으로 등록하지 않고 new로 만든다
        registry.addInterceptor(new LoginCheckInterceptor())
                .order(1)                       // 나중에 관리자 인터셉터가 생기면 로그인 검사가 먼저
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/members",         // 회원가입 (/api/members/me는 다른 경로라 제외되지 않음)
                        "/api/login",
                        "/api/logout",          // 세션이 없어도 204로 응답하면 되므로 막을 이유가 없다
                        "/api/products",        // 상품 목록/상세는 누구나 조회
                        "/api/products/**"
                );
    }
}
