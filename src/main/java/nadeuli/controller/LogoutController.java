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
     * ✅ 로그아웃 API (Redis에서 Access Token 삭제, Refresh Token 유지)
     */
    @DeleteMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        // 🚨 예외 처리: Authorization 헤더 확인
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("🚨 [로그아웃 실패] 유효하지 않은 토큰 형식: {}", token);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 토큰 형식입니다.",
                    "status", 400
            ));
        }

        // 🔹 AccessToken에서 "Bearer " 제거
        String accessToken = token.substring(7);
        log.info("🔹 [로그아웃] 요청된 Access Token: {}", accessToken);

        // ✅ JWT에서 사용자 이메일 추출
        try {
            if (!jwtTokenService.validateToken(accessToken)) {
                log.warn("🚨 [로그아웃 실패] 유효하지 않은 JWT");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "🚨 유효하지 않은 JWT입니다. 다시 로그인해주세요."
                ));
            }
        } catch (Exception e) {
            log.error("🚨 [로그아웃 실패] JWT 검증 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "🚨 JWT 검증 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }

        String userEmail = jwtTokenService.getUserEmail(accessToken);

        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("🚨 [로그아웃 실패] 이메일 추출 실패 - token: {}", accessToken);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 Access Token",
                    "status", 400
            ));
        }

        // ✅ Redis에서 Access Token 삭제
        boolean accessDeleted = jwtTokenService.deleteAccessToken(userEmail);

        if (!accessDeleted) {
            log.warn("⚠️ [로그아웃] Access Token 삭제 실패 또는 존재하지 않음 - userEmail: {}", userEmail);
        }

        log.info("✅ [로그아웃 완료] - userEmail: {}, AccessToken 삭제: {}", userEmail, accessDeleted);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ 로그아웃 완료",
                "accessTokenDeleted", accessDeleted
        ));
    }
}