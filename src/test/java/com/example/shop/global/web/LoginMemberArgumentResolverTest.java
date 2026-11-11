package com.example.shop.global.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스프링 없이 리졸버만 직접 호출하는 단위 테스트.
 * "어떤 파라미터를 처리하는가(supportsParameter)"는 MockMvc로 확인하기 어려워서 직접 불러 본다.
 */
class LoginMemberArgumentResolverTest {

    LoginMemberArgumentResolver resolver = new LoginMemberArgumentResolver();

    /** 리플렉션으로 아래 메서드의 파라미터 정보를 꺼내기 위한 가짜 컨트롤러 메서드들 */
    @SuppressWarnings("unused")
    static class Sample {
        void withLogin(@Login Long memberId) {}
        void withoutLogin(Long memberId) {}
        void wrongType(@Login String memberId) {}
    }

    MethodParameter param(String methodName, Class<?> type) throws Exception {
        Method method = Sample.class.getDeclaredMethod(methodName, type);
        return new MethodParameter(method, 0);   // 0번째 파라미터
    }

    @Test
    void Login이_붙은_Long_파라미터만_처리한다() throws Exception {
        assertThat(resolver.supportsParameter(param("withLogin", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(param("withoutLogin", Long.class))).isFalse();
        assertThat(resolver.supportsParameter(param("wrongType", String.class))).isFalse();
    }

    @Test
    void 세션의_회원_id를_꺼내_준다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        request.setSession(session);

        Object result = resolver.resolveArgument(param("withLogin", Long.class), null,
                new ServletWebRequest(request), null);

        assertThat(result).isEqualTo(1L);
    }
}
