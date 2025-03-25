package nadeuli.controller;

/* OpenAITravelController.java
 * OPEN API 연동
 * 해당 파일 설명
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

import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.OpenAITravelRequest;
import nadeuli.dto.OpenAITravelResponse;
import nadeuli.service.OpenAITravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/travel", produces = "application/json")
@RequiredArgsConstructor
public class OpenAITravelController {

    private final OpenAITravelService travelService;

    @PostMapping
    public ResponseEntity<OpenAITravelResponse> postRecommendedRoute(@RequestBody OpenAITravelRequest request) {
        log.warn("AI 추천 진입");
        OpenAITravelResponse response = travelService.recommendRoute(request);
        return ResponseEntity.ok(response);
    }
}

