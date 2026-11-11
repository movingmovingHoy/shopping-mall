package com.example.shop.global.web;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 로그인하지 않은 요청을 컨트롤러에 도달하기 전에 막는다.
 * 어떤 URL에 적용할지는 WebMvcConfig에서 정한다. (이 클래스는 "로그인 했나?"만 판단)
 *
 * 응답을 직접 쓰지 않고 BusinessException을 던지는 이유:
 * preHandle에서 던진 예외도 DispatcherServlet이 @RestControllerAdvice(ApiExceptionHandler)로 넘겨 준다.
 * 그래서 다른 오류와 똑같은 JSON 형식({code, message, errors})으로 401이 나간다.
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // getSession(false): 검사만 하면 되므로 세션이 없을 때 새로 만들지 않는다
        HttpSession session = request.getSession(false);

        // 지금은 세션을 만드는 곳이 LoginSessionManager.login() 하나뿐이라 "세션 있음 = 로그인함"이다.
        // 로그인 없이 세션이 생기는 기능(비회원 장바구니 등)을 추가하면 loginMemberId 존재 여부도 검사해야 한다.
        if (session == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return true;   // true → 다음 단계(컨트롤러)로 진행
    }
}
