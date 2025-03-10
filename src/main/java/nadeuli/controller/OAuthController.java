/* OAuthController.java
 * OAuth 로그인 성공 후 JWT 발급 및 사용자 정보 반환
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       OAuth 로그인 처리 및 JWT 발급 초안
 * 국경민      03-05       provider 값을 OAuth2UserRequest에서 가져오도록 수정
 * 국경민      03-05       JWT 발급 오류 수정 및 디버깅 로그 추가
 * 국경민      03-05       Provider 값 설정 방식 개선 및 예외 처리 추가
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.service.JwtTokenService;
import nadeuli.service.OAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthController {
    private final OAuthService oAuthService;
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ OAuth 로그인 성공 후 JWT 발급 API
     */
    @GetMapping("/loginSuccess")
    public ResponseEntity<Map<String, Object>> loginSuccess() {
        // ✅ Spring Security의 SecurityContextHolder에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 🚨 예외 처리: OAuth2 인증 정보 확인
        if (authentication == null) {
            log.error("🚨 SecurityContextHolder에 인증 정보가 없습니다!");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "🚨 로그인 정보가 없습니다. 다시 로그인해주세요.",
                    "status", 401
            ));
        }

        if (!(authentication instanceof OAuth2AuthenticationToken authToken)) {
            log.error("🚨 OAuth2 인증 정보가 없습니다! 로그인 과정에서 문제가 발생함.");
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "🚨 OAuth2 인증을 먼저 수행해야 합니다!",
                    "status", 400
            ));
        }

        // ✅ OAuth2User 가져오기
        OAuth2User user = authToken.getPrincipal();
        String provider = authToken.getAuthorizedClientRegistrationId();
        log.info("🔹 OAuth 로그인 요청 - Provider: {}", provider);

        // ✅ OAuth 사용자 정보 처리
        UserDTO userDTO = oAuthService.processOAuthUser(user, provider, authToken);

        try {
            // ✅ 기존 Refresh Token 확인
            String existingRefreshToken = jwtTokenService.getRefreshToken(userDTO.getUserEmail());

            // ✅ 새로운 JWT 발급
            String accessToken = jwtTokenService.createAccessToken(userDTO.getUserEmail());
            String refreshToken = (existingRefreshToken != null) ? existingRefreshToken : jwtTokenService.createRefreshToken(userDTO.getUserEmail());

            // ✅ Redis에 토큰 저장 (새로운 Refresh Token이 있을 경우 업데이트)
            jwtTokenService.storeToken("accessToken:" + userDTO.getUserEmail(), accessToken, 30 * 60 * 1000L); // 30분
            if (existingRefreshToken == null) {
                jwtTokenService.storeToken("refreshToken:" + userDTO.getUserEmail(), refreshToken, 7 * 24 * 60 * 60 * 1000L); // 7일
            }

            log.info("✅ JWT 발급 완료 - Email: {}, Access Token: [HIDDEN], Refresh Token: [HIDDEN]", userDTO.getUserEmail());

            // ✅ JSON 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "accessTokenExpiresIn", "30분",
                    "refreshTokenExpiresIn", "7일",
                    "userEmail", userDTO.getUserEmail(),
                    "userName", userDTO.getUserName(),
                    "profileImage", userDTO.getProfileImage(),
                    "provider", provider
            ));
        } catch (Exception e) {
            log.error("🚨 JWT 발급 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "🚨 JWT 발급 중 오류가 발생했습니다.",
                    "details", e.getMessage(),
                    "status", 500
            ));
        }
    }
}

