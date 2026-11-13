package com.team1.crm.analytics.ai;

import com.team1.crm.analytics.AnalyticsService.GradeCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 외부 호출 없이 실제 OpenRouter 요청과 오류 처리를 검사한다. 작성자 한상희, 작성일 2026-11-12 */
class AiMarketingServiceTest {
    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final String COMPLETIONS_URL = BASE_URL + "/chat/completions";
    private static final GradeCount SEGMENT = new GradeCount("단골", 2);
    private static final List<String> MESSAGES = List.of(
            "지난 방문에 감사드려요.",
            "시간 나실 때 편하게 들러 주세요.",
            "다시 뵐 날을 기다리겠습니다.");
    private static final String VALID_RESPONSE = """
            {"choices":[{"finish_reason":"stop","message":{"content":
            "지난 방문에 감사드려요.\\n시간 나실 때 편하게 들러 주세요.\\n다시 뵐 날을 기다리겠습니다."}}]}
            """;

    private MockRestServiceServer server;
    private RestClient client;
    private AiMarketingService service;

    /** 가짜 서버를 실제 요청 클라이언트에 연결한다. 작성자 한상희, 작성일 2026-11-12 */
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
        service = new AiMarketingService("test-key", "nvidia/nemotron-3.5-super-vl:free", client);
    }

    /** 외부 요청의 사용자 메시지가 허용한 집계 정보로만 구성되는지 확인한다. 작성자 한상희, 작성일 2026-11-12 */
    @Test
    void sendsOnlyAllowedMarketingContextAndReturnsThreeMessages() {
        server.expect(requestTo(COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("""
                        매장 업종: 카페
                        고객 등급: 단골
                        해당 등급 인원: 2명
                        요청 주제: 지난 방문에 대한 감사
                        """))
                .andExpect(jsonPath("$.reasoning.enabled").value(false))
                .andRespond(withSuccess(VALID_RESPONSE, MediaType.APPLICATION_JSON));

        var result = service.generate(SEGMENT, "카페", 4);
        assertEquals(MESSAGES, result.messages());
        assertFalse(result.example());
        server.verify();
    }

    /** 문구가 부족하면 한 번만 형식을 강조해 다시 요청한다. 작성자 한상희, 작성일 2026-11-12 */
    @Test
    void retriesWhenTheResponseHasTooFewMessages() {
        server.expect(requestTo(COMPLETIONS_URL))
                .andExpect(content().string(not(containsString("완성 문장 세 줄만"))))
                .andRespond(withSuccess("""
                        {"choices":[{"finish_reason":"stop","message":{"content":"지난 방문에 감사드려요."}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(COMPLETIONS_URL))
                .andExpect(jsonPath("$.messages[0].content", containsString("완성 문장 세 줄만")))
                .andRespond(withSuccess(VALID_RESPONSE, MediaType.APPLICATION_JSON));

        assertEquals(MESSAGES, service.generate(SEGMENT, "카페", 4).messages());
        server.verify();
    }

    /** OpenRouter의 429는 재요청 없이 제한 안내로 전달한다. 작성자 한상희, 작성일 2026-11-12 */
    @Test
    void rateLimitDoesNotRetry() {
        server.expect(requestTo(COMPLETIONS_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        var error = assertThrows(AiUnavailableException.class, () -> service.generate(SEGMENT, "카페", 4));
        assertEquals("OpenRouter의 무료 모델 요청 한도에 걸렸습니다. 잠시 후 다시 시도해 주세요.", error.getMessage());
        server.verify();
    }

    /** 유료 모델을 지정하면 HTTP 요청 전에 거부한다. 작성자 한상희, 작성일 2026-11-12 */
    @Test
    void paidModelIsRejectedBeforeAnyRequest() {
        var paidService = new AiMarketingService("test-key", "nvidia/nemotron-3.5-super-vl", client);

        assertThrows(AiUnavailableException.class, () -> paidService.generate(SEGMENT, "카페", 4));
        server.verify();
    }
}
