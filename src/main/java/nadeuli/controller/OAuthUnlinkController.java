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
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.JwtTokenService;
import nadeuli.service.OAuthUnlinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            @RequestHeader("Authorization") String token) {

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("🚨 [OAuthUnlink] 유효하지 않은 토큰 형식 - token: {}", token);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 토큰 형식입니다."
            ));
        }

        // 1️⃣ JWT에서 사용자 이메일 추출 전, 토큰 검증
        String accessToken = token.replace("Bearer ", "");

        if (!jwtTokenService.validateToken(accessToken)) {
            log.warn("🚨 [OAuthUnlink] 유효하지 않은 JWT - token: {}", accessToken);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "🚨 유효하지 않은 JWT입니다. 다시 로그인해주세요."
            ));
        }

        String authenticatedEmail = jwtTokenService.getUserEmail(accessToken);

        if (authenticatedEmail == null || !email.equals(authenticatedEmail)) {
            log.warn("🚨 [OAuthUnlink] 본인 계정만 탈퇴할 수 있습니다. 요청: {}, 인증된 이메일: {}", email, authenticatedEmail);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "🚨 본인 계정만 탈퇴할 수 있습니다."
            ));
        }

        // 2️⃣ OAuth 계정 해제 및 MySQL 삭제
        boolean isDeleted = oAuthUnlinkService.unlinkAndDeleteUser(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("success", isDeleted);

        if (isDeleted) {
            // ✅ Redis에서 JWT 삭제 추가
            boolean accessDeleted = jwtTokenService.deleteTokens("accessToken:" + email);
            boolean refreshDeleted = jwtTokenService.deleteTokens("refreshToken:" + email);
            log.info("✅ [OAuthUnlink] 회원 탈퇴 완료 - Email: {}, AccessToken 삭제: {}, RefreshToken 삭제: {}",
                    email, accessDeleted, refreshDeleted);

            response.put("message", "✅ OAuth 계정 해제 및 회원 탈퇴 완료");
            response.put("accessTokenDeleted", accessDeleted);
            response.put("refreshTokenDeleted", refreshDeleted);
            return ResponseEntity.ok(response);
        }

        log.error("🚨 [OAuthUnlink] 회원 탈퇴 실패 - Email: {}", email);
        response.put("message", "❌ 회원 탈퇴 실패");
        return ResponseEntity.status(500).body(response);
    }
}

