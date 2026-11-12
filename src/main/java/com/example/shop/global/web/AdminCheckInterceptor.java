package com.example.shop.global.web;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 관리자가 아닌 회원의 요청을 막는다. 적용 경로(/api/admin/**)는 WebMvcConfig에서 정한다.
 *
 * - LoginCheckInterceptor(order 1)가 먼저 실행되므로, 여기 오는 요청은 이미 로그인한 상태다.
 *   그래도 순서 설정을 잘못 바꾸는 실수에 대비해 세션이 없으면 LOGIN_REQUIRED로 막는다.
 * - role은 로그인할 때 LoginSessionManager가 세션에 넣어 둔 값을 쓴다. (DB 조회 없음)
 */
public class AdminCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }

        // enum 상수는 값마다 객체가 하나뿐이라 == 로 비교해도 안전하다.
        // role이 null이어도(세션에 role이 없음) NPE 없이 "관리자 아님"으로 처리된다.
        Object role = session.getAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE);
        if (role != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ADMIN_REQUIRED);
        }
        return true;
    }
}
