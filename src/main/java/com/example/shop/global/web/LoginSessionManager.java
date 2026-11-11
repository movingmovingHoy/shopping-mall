package com.example.shop.global.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * 로그인 세션을 만들고 없애는 일을 한곳에 모은다.
 * - 컨트롤러는 HttpSession을 직접 다루지 않고 이 클래스만 호출한다. (CLAUDE.md 규칙)
 * - 세션 키(LOGIN_MEMBER_ID)도 여기 한 곳에서 관리한다.
 *   다음에 만들 로그인 인터셉터와 @Login ArgumentResolver도 이 키로 세션을 읽는다.
 * - 세션에는 Member 객체가 아니라 회원 id만 저장한다.
 *   (서버 메모리 절약, 회원 정보가 바뀌어도 세션에 옛날 값이 남지 않음, password 해시가 세션에 들어가지 않음)
 */
@Component
public class LoginSessionManager {

    public static final String LOGIN_MEMBER_ID = "loginMemberId";

    public void login(HttpServletRequest request, Long memberId) {
        // 세션 고정 공격 방지: 로그인 전에 쓰던 세션이 있으면 버리고 새 세션(새 JSESSIONID)을 만든다.
        // 공격자가 미리 심어 둔 세션 id로 피해자가 로그인하면, 공격자도 그 세션으로 로그인 상태가 되기 때문이다.
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate(); // 세션 삭제.
        }

        HttpSession session = request.getSession(true);   // 새 세션 생성 → 응답에 Set-Cookie: JSESSIONID
        session.setAttribute(LOGIN_MEMBER_ID, memberId);
    }

    public void logout(HttpServletRequest request) {
        // getSession(false): 세션이 없으면 새로 만들지 않고 null을 준다. 로그아웃하려고 세션을 만들 필요는 없다.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 삭제.
        }
    }
}
