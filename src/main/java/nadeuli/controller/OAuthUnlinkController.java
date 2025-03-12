/* OAuthUnlinkController.java
 * OAuth 계정 해제 및 회원 탈퇴 API
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       회원 탈퇴 API 초안
 * 국경민      03-05       JWT 인증 추가 (본인 계정만 탈퇴 가능하도록 변경)
 * 국경민      03-12       JWT 검증 방식 개선 및 예외 처리 강화
 * 국경민      03-12       불필요한 `null` 체크 제거 및 코드 최적화
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.JwtTokenService;
import nadeuli.service.OAuthUnlinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthUnlinkController {

    private final OAuthUnlinkService oAuthUnlinkService;
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ OAuth 계정 해제 및 회원 탈퇴 API
     */
    @DeleteMapping("/unlink/{email}")
    public ResponseEntity<Map<String, Object>> unlinkUser(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("🚨 [OAuthUnlink] 유효하지 않은 토큰 형식");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 토큰 형식입니다."
            ));
        }

        // 1️⃣ JWT 검증 및 사용자 인증
        String accessToken = token.substring(7); // "Bearer " 제거

        try {
            if (!jwtTokenService.validateToken(accessToken)) {
                log.warn("🚨 [OAuthUnlink] 유효하지 않은 JWT");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "🚨 유효하지 않은 JWT입니다. 다시 로그인해주세요."
                ));
            }
        } catch (Exception e) {
            log.error("🚨 [OAuthUnlink] JWT 검증 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "🚨 JWT 검증 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }

        String authenticatedEmail = jwtTokenService.getUserEmail(accessToken);

        if (!email.equals(authenticatedEmail)) { // ✅ 불필요한 `null` 체크 제거
            log.warn("🚨 [OAuthUnlink] 본인 계정만 탈퇴할 수 있습니다. 요청: {}, 인증된 이메일: {}", email, authenticatedEmail);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "🚨 본인 계정만 탈퇴할 수 있습니다."
            ));
        }

        // 2️⃣ OAuth 계정 해제 및 MySQL 삭제
        boolean isDeleted;
        try {
            isDeleted = oAuthUnlinkService.unlinkAndDeleteUser(email);
        } catch (Exception e) {
            log.error("🚨 [OAuthUnlink] 회원 탈퇴 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "🚨 회원 탈퇴 처리 중 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }

        if (isDeleted) {
            // ✅ Redis에서 JWT 삭제 추가
            boolean accessDeleted = jwtTokenService.deleteTokens("accessToken:" + email);
            boolean refreshDeleted = jwtTokenService.deleteTokens("refreshToken:" + email);
            log.info("✅ [OAuthUnlink] 회원 탈퇴 완료 - Email: {}, AccessToken 삭제: {}, RefreshToken 삭제: {}",
                    email, accessDeleted, refreshDeleted);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "email", email,
                    "message", "✅ OAuth 계정 해제 및 회원 탈퇴 완료",
                    "accessTokenDeleted", accessDeleted,
                    "refreshTokenDeleted", refreshDeleted
            ));
        }

        log.error("🚨 [OAuthUnlink] 회원 탈퇴 실패 - Email: {}", email);
        return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "email", email,
                "message", "❌ 회원 탈퇴 실패"
        ));
    }
}
