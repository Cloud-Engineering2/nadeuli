/* LogoutController.java
 * JWT 삭제 및 로그아웃 API
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       로그아웃 API 초안
 * 국경민      03-05       Redis에서 JWT 삭제 기능 추가
 * 국경민      03-06       JWT 검증 방식 개선 및 로그 추가
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ 로그아웃 API (Redis에서 JWT 삭제)
     */
    @DeleteMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        // 🚨 예외 처리: Authorization 헤더 확인
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("🚨 [로그아웃 실패] 유효하지 않은 토큰 형식 - token: {}", token);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 토큰 형식입니다.",
                    "status", 400
            ));
        }

        // 🔹 AccessToken에서 "Bearer " 제거
        String accessToken = token.replace("Bearer ", "");

        // ✅ JWT에서 사용자 이메일 추출 (만료된 토큰에서도 이메일 가져오기 가능하도록 수정)
        String userEmail = jwtTokenService.getUserEmail(accessToken);

        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("🚨 [로그아웃 실패] 이메일 추출 실패 - token: {}", accessToken);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 Access Token",
                    "status", 400
            ));
        }

        // ✅ Redis에서 토큰 삭제
        boolean accessDeleted = jwtTokenService.deleteTokens("accessToken:" + userEmail);
        boolean refreshDeleted = jwtTokenService.deleteTokens("refreshToken:" + userEmail);

        // ✅ 로그아웃 성공 처리 (이미 삭제된 경우도 성공으로 간주)
        log.info("✅ 로그아웃 완료 - userEmail: {}, AccessToken 삭제: {}, RefreshToken 삭제: {}",
                userEmail, accessDeleted, refreshDeleted);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ 로그아웃 완료",
                "accessTokenDeleted", accessDeleted,
                "refreshTokenDeleted", refreshDeleted
        ));
    }
}

