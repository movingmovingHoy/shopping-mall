package com.example.shop.global.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 아직 로그인이 필요한 진짜 API가 없어서 테스트 전용 컨트롤러로 확인한다.
 * - @WebMvcTest는 WebMvcConfigurer(WebMvcConfig)를 함께 띄우므로, 실제 등록된 경로 규칙 그대로 검사된다.
 * - 테스트 전용 컨트롤러는 진짜 컨트롤러와 같은 URL을 써서 "이 경로가 열려 있나/막혀 있나"를 확인한다.
 *   (controllers = TestController.class라서 진짜 MemberController 등은 뜨지 않아 URL이 겹쳐도 충돌하지 않는다)
 */
@WebMvcTest(controllers = LoginCheckInterceptorTest.TestController.class)
@Import(LoginCheckInterceptorTest.TestController.class)
class LoginCheckInterceptorTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 세션이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void 로그인했으면_통과() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        mockMvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void 제외한_경로는_로그인_없이_통과() throws Exception {
        mockMvc.perform(post("/api/members")).andExpect(status().isOk());
        mockMvc.perform(post("/api/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/logout")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/1")).andExpect(status().isOk());
    }

    @Test
    void 관리자_상품_API는_products_제외에_걸리지_않는다() throws Exception {
        // "/api/products/**" 제외가 "/api/admin/products"까지 열어 버리지 않는지 확인
        mockMvc.perform(post("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/members/me")
        String me() { return "ok"; }

        @PostMapping("/api/members")
        String signup() { return "ok"; }

        @PostMapping("/api/login")
        String login() { return "ok"; }

        @PostMapping("/api/logout")
        String logout() { return "ok"; }

        @GetMapping("/api/products")
        String products() { return "ok"; }

        @GetMapping("/api/products/{id}")
        String product() { return "ok"; }

        @PostMapping("/api/admin/products")
        String adminProduct() { return "ok"; }
    }
}
