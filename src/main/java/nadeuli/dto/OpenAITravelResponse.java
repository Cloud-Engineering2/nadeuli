package nadeuli.dto;

/* OpenAITravelResponse.java
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAITravelResponse {
    private Map<String, List<String>> recommendedRoute;
}