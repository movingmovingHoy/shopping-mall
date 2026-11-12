package com.example.shop.global.config;

import com.example.shop.global.web.AdminCheckInterceptor;
import com.example.shop.global.web.LoginCheckInterceptor;
import com.example.shop.global.web.LoginMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 인터셉터(로그인 검사, 관리자 검사)와 @Login ArgumentResolver 등록.
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

        // 관리자 API만 검사한다. /api/admin/** 은 위에서 제외하지 않았으므로 로그인 검사를 먼저 통과한 요청만 온다.
        registry.addInterceptor(new AdminCheckInterceptor())
                .order(2)
                .addPathPatterns("/api/admin/**");
    }

    /** 컨트롤러 파라미터의 @Login Long memberId에 값을 넣어 줄 리졸버를 등록한다 */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
