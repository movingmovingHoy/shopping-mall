package com.example.shop.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트 전용 컨트롤러로 각 예외를 일부러 발생시켜
 * ApiExceptionHandler가 올바른 상태 코드와 JSON을 응답하는지 확인한다.
 * @WebMvcTest는 웹 계층만 띄우므로 DB 연결 없이 빠르게 실행된다.
 */
@WebMvcTest(controllers = ApiExceptionHandlerTest.TestController.class)
@Import(ApiExceptionHandlerTest.TestController.class)
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 비즈니스_예외는_ErrorCode의_상태와_메시지로_응답한다() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void 검증_실패시_필드별_오류를_응답한다() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void JSON_문법이_틀리면_400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{email:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 파라미터_타입이_틀리면_400() throws Exception {
        mockMvc.perform(get("/test/type").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 없는_URL은_404() throws Exception {
        mockMvc.perform(get("/no-such-url"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 지원하지_않는_메서드는_405() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 예상하지_못한_예외는_500이고_내부_메시지를_숨긴다() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    /*
     * 요청 DTO: Jackson이 기본 생성자로 객체를 만든 뒤 필드에 값을 채운다.
     * 그래서 필드에 final을 붙일 수 없고, 기본 생성자가 필요하다.
     */
    @Getter
    @NoArgsConstructor
    static class TestRequest {

        @NotBlank
        @Email
        private String email;
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/type")
        void type(@RequestParam int page) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("DB 비밀번호 같은 내부 정보");
        }
    }
}
