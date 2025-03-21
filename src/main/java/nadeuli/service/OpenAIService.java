package nadeuli.service;

/* OpenAIService.java
 * OPEN API 연동
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-21
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private final OpenAiService openAiService;

    public OpenAIService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String getRecommendedRoute(String userRoute, String mode, Map<String, List<String>> fixedDays) {
        String prompt;

        if ("J".equalsIgnoreCase(mode) && fixedDays != null && !fixedDays.isEmpty()) {
            StringBuilder fixedInfo = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : fixedDays.entrySet()) {
                String date = entry.getKey();
                List<String> places = entry.getValue();
                fixedInfo.append(String.format("- %s: %s\n", date, places));
            }

            prompt = String.format(
                    "다음은 전체 여행 경로입니다:\n%s\n" +
                            "특정 날짜에 반드시 포함되어야 할 장소는 다음과 같습니다:\n%s\n\n" +
                            "**반드시 JSON 형식으로만 응답하세요!**\n" +
                            "**예제:**\n" +
                            "{\n  \"1\": [\"placeid1\", \"placeid2\"],\n  \"2\": [\"placeid3\", \"placeid4\"]\n}\n" +
                            "JSON 코드 블록(```)을 사용하지 말고 순수 JSON 형식으로만 응답하세요.",
                    userRoute, fixedInfo.toString()
            );
        } else {
            prompt = String.format(
                    "다음 장소들을 기반으로 최적의 여행 일정을 생성해주세요.\n\n" +
                            "**반드시 JSON 형식으로만 응답하세요!**\n" +
                            "**예제:**\n" +
                            "{\n  \"1\": [\"placeid1\", \"placeid2\"],\n  \"2\": [\"placeid3\", \"placeid4\"]\n}\n" +
                            "JSON 코드 블록(```)을 사용하지 말고 순수 JSON 형식으로만 응답하세요.",
                    userRoute
            );
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(500)
                .temperature(0.7)
                .build();

        var result = openAiService.createChatCompletion(request);
        String response = result.getChoices().get(0).getMessage().getContent().trim();

        System.out.println("GPT 응답 (Raw):\n" + response);

        if (response.startsWith("```json")) {
            response = response.replace("```json", "").replace("```", "").trim();
        } else if (response.startsWith("```")) {
            response = response.replace("```", "").trim();
        }

        System.out.println("GPT 응답 (Processed JSON):\n" + response);

        return response;
    }
}


