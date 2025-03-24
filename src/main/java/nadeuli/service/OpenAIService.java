package nadeuli.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAiService openAiService;

    public String getRecommendedRoute(List<PlaceDTO> places, String mode, Map<String, List<String>> fixedDays) {
        String prompt;

        // 1. 장소 리스트를 프롬프트용 문자열로 변환
        String userRoute = places.stream()
                .map(PlaceDTO::toPromptString)
                .collect(Collectors.joining("\n"));

        // 2. 고정 일정 텍스트 생성
        StringBuilder fixedInfo = new StringBuilder();
        if (fixedDays != null && !fixedDays.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : fixedDays.entrySet()) {
                String date = entry.getKey();
                List<String> placeIds = entry.getValue();
                fixedInfo.append(String.format("- %s: %s\n", date, placeIds));
            }
        }

        // 3. 프롬프트 생성
        if ("J".equalsIgnoreCase(mode) && fixedInfo.length() > 0) {
            prompt = String.format(
                    "다음은 여행 장소 리스트입니다:\n%s\n\n" +
                            "여행 일정 조건은 다음과 같습니다:\n" +
                            "총 3일 여행이며, 시작 시간은 아래와 같습니다.\n" +
                            "1일차: 09:00 / 2일차: 10:00 / 3일차: 09:00\n" +
                            "숙소가 존재하는 경우 아래 조건을 따릅니다:\n" +
                            "1일차: 숙소는 마지막 장소\n" +
                            "2일차: 숙소는 시작과 종료 모두 포함\n" +
                            "3일차: 숙소는 시작 장소에만 포함, 마지막은 공항\n" +
                            "숙소의 stay-minute은 무조건 0입니다.\n" +
                            "장소의 상대적 거리(위도/경도)를 고려해 동선을 최소화해주세요.\n" +
                            "장소 타입은 고려하되, 동선 최적화가 우선입니다.\n" +
                            "일반 관광지: 평균 60~90분, 자연경관은 90분, 공항은 30분 기준입니다.\n\n" +
                            "특정 날짜에 반드시 포함되어야 할 장소는 다음과 같습니다:\n%s\n\n" +
                            "반드시 아래와 같은 순수 JSON 형식으로만 응답해주세요. 설명도 없이 출력만:\n\n" +
                            "{\n" +
                            "  \"2025-03-24\": [\"abcabc1\", \"abcabc2\"],\n" +
                            "  \"2025-03-25\": [\"abcabc3\", \"abcabc4\"]\n" +
                            "}\n\n" +
                            "JSON 코드블록(```json) 없이 순수 JSON 객체만 반환해주세요.",
                    userRoute,
                    fixedInfo
            );
        } else {
            prompt = String.format(
                    "다음은 여행 장소 리스트입니다:\n%s\n\n" +
                            "위 장소들을 기반으로 동선을 최소화한 최적의 3일 여행 일정을 추천해주세요.\n" +
                            "※ 결과는 반드시 아래와 같은 JSON 구조로만 출력해주세요. 설명 없이 출력만 해주세요.\n" +
                            "[\n  {\n    \"day\": 1,\n    \"start-time\": \"09:00\",\n    \"places\": [\n      { \"name\": \"장소명\", \"place-id\": \"placeid\", \"stay-minute\": \"00:30\" }\n    ]\n  }\n]",
                    userRoute
            );
        }

        // 4. GPT 요청 생성 및 호출
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(1000)
                .temperature(0.7)
                .build();

        var result = openAiService.createChatCompletion(request);
        String response = result.getChoices().get(0).getMessage().getContent().trim();

        // 5. GPT 응답에서 불필요한 코드블록 제거
        if (response.startsWith("```json")) {
            response = response.replace("```json", "").replace("```", "").trim();
        } else if (response.startsWith("```")) {
            response = response.replace("```", "").trim();
        }

        return response;
    }
}
