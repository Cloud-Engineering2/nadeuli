/* RequiredArgsConstructor.java
 * 새로운 Refresh Token 갱신 API 추가
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-07       RequiredArgsConstructor 초안
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    /**
     * ✅ Refresh Token을 사용하여 새로운 Access Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAccessToken(@RequestHeader(value = "Authorization", required = false) String token) {
        // 🚨 예외 처리: Authorization 헤더 확인
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("🚨 [Refresh Token] 유효하지 않은 토큰 형식: {}", token);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 토큰 형식입니다.",
                    "error", "Invalid token format"
            ));
        }

        // 🔹 Refresh Token에서 "Bearer " 제거
        String refreshToken = token.substring(7);

        // ✅ RefreshTokenService를 통해 새로운 Access Token 발급
        try {
            Map<String, Object> tokenResponse = refreshTokenService.refreshAccessToken(refreshToken);

            // 🚨 예외 처리: Refresh Token이 유효하지 않을 경우
            if (!Boolean.TRUE.equals(tokenResponse.get("success"))) {
                log.warn("🚨 [Refresh Token] 유효하지 않은 Refresh Token 요청 - 응답: {}", tokenResponse);
                return ResponseEntity.status(403).body(tokenResponse);
            }

            // ✅ JSON 응답 반환
            log.info("✅ [Refresh Token] 새로운 Access Token 발급 완료 - 응답: {}", tokenResponse);
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("🚨 [Refresh Token] Access Token 재발급 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "🚨 Access Token 재발급 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }
    }
}
