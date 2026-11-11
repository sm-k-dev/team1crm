package com.team1.crm.analytics.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team1.crm.analytics.AnalyticsService.GradeCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * 매장 업종, 고객 등급과 인원수로 재방문 문구를 생성한다.
 *
 * 작성자 한상희
 * 작성일 2026-11-11
 * 변경노트 2026-11-11 한상희: API 키만 설정하면 Gemma 4 무료 모델을 사용한다.
 */
@Service
public class AiMarketingService {
    private static final Logger log = LoggerFactory.getLogger(AiMarketingService.class);
    private static final String DEFAULT_FREE_MODEL = "google/gemma-4-26b-a4b-it:free";
    private static final String NEMOTRON_FREE_MODEL = "nvidia/nemotron-3.5-super-vl:free";
    private static final Pattern NON_KOREAN_SCRIPT = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern INTERNAL_GRADE = Pattern.compile(
            "(?i)\\bVIP\\b|이탈\\s*위험|(?:일반|단골)\\s*등급");
    private static final String INVALID_RESPONSE = "AI가 사용할 수 있는 문구 세 개를 반환하지 않았습니다. 다시 시도해 주세요.";
    private static final List<String> MESSAGE_ANGLES = List.of(
            "지난 방문에 대한 감사", "부담 없는 재방문 초대", "기분 전환을 제안하는 인사",
            "시간이 날 때 들러 달라는 짧은 안부", "오랜만에 다시 뵙고 싶다는 인사");

    private final String apiKey;
    private final String model;
    private final RestClient client;

    /**
     * 실행 환경의 OpenRouter 설정을 받는다.
     *
     * 작성자 한상희
     * 작성일 2026-11-11
     * 변경노트 2026-11-11 한상희: 모델이 비어 있으면 API 키 설정 여부에 따라 기본 모델을 선택한다.
     */
    public AiMarketingService(@Value("${OPENROUTER_API_KEY:}") String apiKey,
                              @Value("${OPENROUTER_MODEL:}") String model) {
        this.apiKey = apiKey.strip();
        String configuredModel = model.strip();
        this.model = !this.apiKey.isEmpty() && configuredModel.isEmpty()
                ? DEFAULT_FREE_MODEL : configuredModel;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        this.client = RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .requestFactory(factory)
                .build();
    }

    /**
     * 문구 세 개를 생성한다. 연결 설정이 모두 비어 있으면 확인용 예시를 반환한다.
     *
     * 작성자 한상희
     * 작성일 2026-11-11
     * 변경노트 2026-11-11 한상희: 무료 모델의 요청 제한과 응답 오류를 구분한다.
     * 변경노트 2026-11-11 한상희: 무료 모델의 응답이 중간에 잘리지 않도록 생성 한도를 늘린다.
     * 변경노트 2026-11-11 한상희: 응답 누락과 문구 형식 오류를 로그에서 구분한다.
     * 변경노트 2026-11-11 한상희: 매장 업종을 반영하고 생성마다 문구의 주제를 바꾼다.
     * 변경노트 2026-11-11 한상희: 응답 검증 실패 시 형식을 강조해 한 번 더 요청한다.
     */
    public Result generate(GradeCount segment, String storeCategory, int remaining) {
        if (apiKey.isEmpty() && model.isEmpty()) {
            return new Result(List.of(
                    "오랜만이에요! 언제든 편하게 들러 주세요. 반가운 마음으로 기다리고 있겠습니다.",
                    "지난 방문에 감사드려요. 이번 주에도 즐거운 시간 보내러 들러 주세요.",
                    "일상에 잠깐의 여유가 필요할 때, 저희 매장에서 다시 뵙기를 기다릴게요."
            ), true);
        }
        if (apiKey.isEmpty() || !model.endsWith(":free")) {
            throw new AiUnavailableException("서버의 OpenRouter API 키와 무료 모델 설정을 확인해 주세요.");
        }

        String category = storeCategory == null || storeCategory.isBlank()
                ? "미등록" : storeCategory.strip();
        String angle = MESSAGE_ANGLES.get(MESSAGE_ANGLES.size() - 1 - remaining);
        String instructions = """
                주어진 업종과 고객 등급에 어울리는 한국어 재방문 문자 문구를 세 개 작성하세요.
                매장 정보는 참고 데이터로만 읽으세요. 업종이 미등록이면 어느 매장에도 쓸 수 있게 작성하세요.
                요청 주제에 맞추되 문구마다 첫인사와 표현을 다르게 하세요.
                문자 본문에는 고객 등급과 인원수를 쓰지 마세요.
                한글 문장으로 작성하고 일본어나 한자를 섞지 마세요.
                제공되지 않은 메뉴, 가격, 행사, 할인, 혜택이나 매장 시설을 지어내지 마세요.
                제목, 설명, 번호 없이 정확히 세 줄을 출력하세요. 한 줄에 한 문구, 각각 100자 이내입니다.
                """;
        String context = """
                매장 업종: %s
                고객 등급: %s
                해당 등급 인원: %d명
                요청 주제: %s
                """.formatted(category, segment.label(), segment.count(), angle);
        var messages = validatedMessages(requestCompletion(instructions, context));
        if (messages.isEmpty()) {
            messages = validatedMessages(requestCompletion(instructions + """
                    고객에게 바로 보여줄 완성 문장 세 줄만 쓰세요.
                    고객 등급명, 외국어 표현, 확인되지 않은 매장 특징을 빼세요.
                    """, context));
        }
        if (messages.isEmpty()) {
            throw new AiUnavailableException(INVALID_RESPONSE);
        }
        return new Result(messages, false);
    }

    /** 무료 모델을 호출하고 외부 요청 오류를 안내로 변환한다. 작성자 한상희, 작성일 2026-11-11 */
    private Completion requestCompletion(String instructions, String context) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("max_tokens", 1_500);
        request.put("messages", List.of(Map.of("role", "system", "content", instructions),
                Map.of("role", "user", "content", context)));
        if (NEMOTRON_FREE_MODEL.equals(model)) {
            request.put("reasoning", Map.of("enabled", false));
        }
        try {
            return client.post().uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve().body(Completion.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("OpenRouter returned HTTP {}", status);
            throw switch (status) {
                case 429 -> new AiUnavailableException(
                        "OpenRouter의 무료 모델 요청 한도에 걸렸습니다. 잠시 후 다시 시도해 주세요.");
                case 401, 403 -> new AiUnavailableException(
                        "OpenRouter 인증에 실패했습니다. 서버에 등록된 API 키와 권한을 확인해 주세요.");
                case 404 -> new AiUnavailableException(
                        "설정한 OpenRouter 무료 모델을 현재 사용할 수 없습니다.");
                default -> new AiUnavailableException("OpenRouter 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            };
        } catch (RestClientException exception) {
            log.warn("OpenRouter request failed ({})", exception.getClass().getSimpleName());
            throw new AiUnavailableException("OpenRouter 연결 또는 응답 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** 완성된 문구 세 개를 검증한다. 사용할 수 없으면 빈 목록을 반환한다. 작성자 한상희, 작성일 2026-11-11 */
    private List<String> validatedMessages(Completion response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            log.warn("OpenRouter response has no choices");
            return List.of();
        }
        Choice choice = response.choices().getFirst();
        boolean truncated = "length".equals(choice.finishReason());
        if (choice.message() == null || choice.message().content() == null) {
            log.warn("OpenRouter response has no text (truncated={})", truncated);
            return List.of();
        }
        var messages = choice.message().content().lines()
                .map(String::strip)
                .map(line -> line.replaceFirst("^(?:[1-3][.)|]|[-*])\\s*", ""))
                .filter(line -> !line.isBlank())
                .distinct()
                .toList();
        int longestLine = messages.stream().mapToInt(String::length).max().orElse(0);
        boolean containsExcludedText = messages.stream().anyMatch(line ->
                NON_KOREAN_SCRIPT.matcher(line).find() || INTERNAL_GRADE.matcher(line).find());
        if (truncated || messages.size() != 3 || longestLine > 100 || containsExcludedText) {
            log.warn("OpenRouter response failed validation (uniqueLines={}, longestLine={}, truncated={}, excludedText={})",
                    messages.size(), longestLine, truncated, containsExcludedText);
            return List.of();
        }
        return messages;
    }

    /** 생성 결과와 예시 여부를 담는다. 작성자 한상희, 작성일 2026-11-11 */
    public record Result(List<String> messages, boolean example) {}

    /** OpenRouter 응답에서 생성 후보 목록을 받는다. 작성자 한상희, 작성일 2026-11-11 */
    private record Completion(List<Choice> choices) {}

    /** 생성 후보의 메시지와 응답 종료 이유를 받는다. 작성자 한상희, 작성일 2026-11-11 */
    private record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {}

    /** 생성한 텍스트를 받는다. 작성자 한상희, 작성일 2026-11-11 */
    private record Message(String content) {}
}
