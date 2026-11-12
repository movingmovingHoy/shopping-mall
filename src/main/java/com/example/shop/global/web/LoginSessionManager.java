package com.example.shop.global.web;

import com.example.shop.member.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * 로그인 세션을 만들고 없애는 일을 한곳에 모은다.
 * - 컨트롤러는 HttpSession을 직접 다루지 않고 이 클래스만 호출한다. (CLAUDE.md 규칙)
 * - 세션 키(LOGIN_MEMBER_ID, LOGIN_MEMBER_ROLE)도 여기 한 곳에서 관리한다.
 *   인터셉터와 @Login ArgumentResolver도 이 키로 세션을 읽는다.
 * - 세션에는 Member 객체가 아니라 회원 id만 저장한다.
 *   (서버 메모리 절약, 회원 정보가 바뀌어도 세션에 옛날 값이 남지 않음, password 해시가 세션에 들어가지 않음)
 * - 예외로 role은 함께 저장한다. 관리자 인터셉터가 요청마다 DB를 조회하지 않게 하기 위해서다.
 *   대가: 로그인 중에 권한이 바뀌면 다시 로그인할 때까지 옛 role이 남는다.
 *   MVP에는 권한을 바꾸는 API가 없어서 이 방식을 택했다.
 *   세션은 서버 메모리에 있고 클라이언트는 JSESSIONID만 가지므로, 클라이언트가 role을 조작할 수는 없다.
 */
@Component
public class LoginSessionManager {

    public static final String LOGIN_MEMBER_ID = "loginMemberId";
    public static final String LOGIN_MEMBER_ROLE = "loginMemberRole";

    public void login(HttpServletRequest request, Long memberId, Role role) {
        // 세션 고정 공격 방지: 로그인 전에 쓰던 세션이 있으면 버리고 새 세션(새 JSESSIONID)을 만든다.
        // 공격자가 미리 심어 둔 세션 id로 피해자가 로그인하면, 공격자도 그 세션으로 로그인 상태가 되기 때문이다.
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate(); // 세션 삭제.
        }

        HttpSession session = request.getSession(true);   // 새 세션 생성 → 응답에 Set-Cookie: JSESSIONID
        session.setAttribute(LOGIN_MEMBER_ID, memberId);
        session.setAttribute(LOGIN_MEMBER_ROLE, role);
    }

    public void logout(HttpServletRequest request) {
        // getSession(false): 세션이 없으면 새로 만들지 않고 null을 준다. 로그아웃하려고 세션을 만들 필요는 없다.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 삭제.
        }
    }
}
