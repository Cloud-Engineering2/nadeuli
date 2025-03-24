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
import nadeuli.dto.OpenAITravelRequest;
import nadeuli.dto.OpenAITravelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAITravelService {

    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    public OpenAITravelResponse recommendRoute(OpenAITravelRequest request) {
        String recommendedRouteJson = openAIService.getRecommendedRoute(
                request.getRoute(),
                request.getMode(),
                request.getFixedDays()
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
}



