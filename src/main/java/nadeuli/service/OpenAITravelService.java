package nadeuli.service;
/* OpenAITravelService.java
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.OpenAITravelRequest;
import nadeuli.dto.OpenAITravelResponse;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAITravelService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    public OpenAITravelResponse recommendRoute(OpenAITravelRequest request) {

        log.warn("getRecommendRoute 호출");
        String recommendedRouteJson = getRecommendedRoute(
                request
        );

        System.out.println("GPT 응답:\n" + recommendedRouteJson);

        try {
            Map<String, List<String>> recommendedRoute = objectMapper.readValue(
                    recommendedRouteJson, new TypeReference<>() {
                    }
            );
            return new OpenAITravelResponse(recommendedRoute);
        } catch (Exception e) {
            System.err.println("🚨 JSON 변환 오류 발생: " + e.getMessage());
            throw new RuntimeException("JSON 변환 오류: " + e.getMessage());
        }
    }


    public String getRecommendedRoute(OpenAITravelRequest req) {
        String prompt;

        log.warn("PlaceDTO 변환중");
        // 1. 장소 리스트를 프롬프트용 문자열로 변환
        try {
            String userRoute = "[" +
                    req.getPlaceDTOList().stream()
                            .collect(Collectors.toMap(
                                    PlaceDTO::getGooglePlaceId,
                                    Function.identity(),
                                    (existing, replacement) -> existing))
                            .values().stream()
                            .map(PlaceDTO::toPromptString)
                            .collect(Collectors.joining(",\n")) +
                    "]";


        // 2. 고정 일정 텍스트 생성
//        StringBuilder fixedInfo = new StringBuilder();
//        if (fixedDays != null && !fixedDays.isEmpty()) {
//            for (Map.Entry<String, List<String>> entry : fixedDays.entrySet()) {
//                String date = entry.getKey();
//                List<String> placeIds = entry.getValue();
//                fixedInfo.append(String.format("- %s: %s\n", date, placeIds));
//            }
//        }
        String fixedInfo = "  ";
            log.warn(userRoute);
        String mode = "J";
        // 3. 프롬프트 생성
        if ("J".equalsIgnoreCase(mode) && fixedInfo.length() > 0) {
            log.warn("J 진입");
            prompt = String.format(
                    "다음은 여행 장소 리스트입니다 (JSON 배열 형식):\n%s\n\n" +
                            "여행 일정 조건은 다음과 같습니다:\n" +
                            "- 총 %s일 여행입니다.\n" +
                            "- 장소 객체는 name, type, latitude, longitude, placeId 정보를 포함합니다.\n" +
                            "- 장소 유형(type)은 다음 중 하나입니다: TRANSPORTATION, RESTAURANT, LODGING, ATTRACTION, LANDMARK, CAFE, CONVENIENCE\n" +
                            "- 숙소(LODGING)는 하루의 시작/종료 지점으로 사용됩니다:\n" +
                            "  • 1일차: 장소 리스트에 공항(TRANSPORTATION)이 있을 경우, 첫 장소로 사용하고 마지막은 숙소\n" +
                            "  • 2~(N-1)일차: 숙소 → 관광지 → 숙소\n" +
                            "  • 마지막 날: 숙소에서 시작해, 공항이 존재할 경우 마지막 장소로 사용\n" +
                            "- 공항이 없다면 숙소만 기준으로 하루 일정을 구성하세요\n" +
                            "- 숙소가 여러 개인 경우, 체크아웃 → 관광 → 체크인 동선을 고려하세요\n" +
                            "- 식사 및 휴식을 위한 장소(예: RESTAURANT, CAFE, CONVENIENCE)는 하루에 1~2개 적절히 배치하세요\n" +
                            "- ATTRACTION, LANDMARK는 주요 관광지이며 하루의 중심 코스가 됩니다.\n" +
                            "- TRANSPORTATION 유형의 장소는 여행 중간에도 포함될 수 있습니다\n" +
                            "- 동일 장소는 여러 날에 반복하여 나타나지 않도록 하세요 (단, 숙소(LODGING)는 반복 허용)\n" +
                            "- 관광지(ATTRACTION, LANDMARK)는 가능한 한 중복 없이 분산 배치하세요\n" +
                            "- 가까운 위치의 장소끼리 묶어서 이동 시간을 줄이도록 동선을 구성하세요\n\n" +
                            "💡 반드시 아래 JSON 형식으로만 응답해주세요 (설명 없이 순수 JSON만 출력):\n" +
                            "{\n" +
                            "  \"day-1\": [\"place-id-1\", \"place-id-2\"],\n" +
                            "  \"day-2\": [\"place-id-3\", \"place-id-4\"]\n" +
                            "  // ... day-N까지\n" +
                            "}",
                    userRoute,
                    req.getItinerary().getTotalDays()
            );


        } else {
            log.warn("P 진입");
            prompt = String.format(
                    "다음은 여행 장소 리스트입니다:\n%s\n\n" +
                            "위 장소들을 기반으로 동선을 최소화한 최적의 3일 여행 일정을 추천해주세요.\n" +
                            "※ 결과는 반드시 아래와 같은 JSON 구조로만 출력해주세요. 설명 없이 출력만 해주세요.\n" +
                            "[\n  {\n    \"day\": 1,\n    \"start-time\": \"09:00\",\n    \"places\": [\n      { \"name\": \"장소명\", \"place-id\": \"placeid\", \"stay-minute\": \"00:30\" }\n    ]\n  }\n]",
                    userRoute
            );
        }

        log.warn("J 호출중");
        // 4. GPT 요청 생성 및 호출
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(1000)
                .temperature(0.7)
                .build();

        log.warn("결과 받는중");
        var result = openAiService.createChatCompletion(request);
        String response = result.getChoices().get(0).getMessage().getContent().trim();

        // 5. GPT 응답에서 불필요한 코드블록 제거
        log.warn("코드블럭 제거중");
        if (response.startsWith("```json")) {
            response = response.replace("```json", "").replace("```", "").trim();
        } else if (response.startsWith("```")) {
            response = response.replace("```", "").trim();
        }

        return response;
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return null;
    }

}



