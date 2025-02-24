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

import nadeuli.dto.OpenAITravelRequest;
import nadeuli.dto.OpenAITravelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenAITravelService {

    private final OpenAIService openAIService;

    public OpenAITravelResponse recommendRoute(OpenAITravelRequest request) {
        String recommendedRoute = openAIService.getRecommendedRoute(request.getRoute());
        return new OpenAITravelResponse(recommendedRoute);
    }
}
