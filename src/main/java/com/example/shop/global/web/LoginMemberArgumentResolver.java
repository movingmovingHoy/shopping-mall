package com.example.shop.global.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @Login Long memberId 파라미터에 세션의 회원 id를 넣어 준다.
 * 덕분에 컨트롤러는 HttpSession을 몰라도 된다. (CLAUDE.md: 컨트롤러에서 HttpSession 직접 다루지 않기)
 *
 * Member 객체가 아니라 id(Long)만 넘기는 이유:
 * - 세션에는 id만 저장되어 있다. Member를 넘기려면 리졸버가 매 요청마다 DB를 조회해야 한다.
 * - 회원 정보가 필요한 기능만 서비스에서 id로 조회하면 된다. (장바구니·주문은 대부분 id만 있으면 충분)
 */
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /** 이 리졸버가 처리할 파라미터인가? @Login이 붙어 있고 타입이 Long이면 처리한다 */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class)
                && Long.class.equals(parameter.getParameterType());
    }

    /**
     * 실제로 파라미터에 넣을 값을 만든다.
     * 로그인 여부는 LoginCheckInterceptor가 먼저 확인하므로 여기서는 세션이 있다고 보고 꺼내기만 한다.
     * (인터셉터 제외 경로에 @Login을 쓰면 세션이 없어 NullPointerException이 난다 → 그런 곳에는 쓰지 않는다)
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return request.getSession(false).getAttribute(LoginSessionManager.LOGIN_MEMBER_ID);
    }
}
