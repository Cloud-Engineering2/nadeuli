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
 * 국경민      03-12       SecurityContext 중복 호출 제거 및 코드 최적화
 * 국경민      03-12       Refresh Token 저장 로직 개선 및 예외 처리 강화
 * 국경민      03-12       Optional 적용 및 JWT 저장 방식 최적화
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
import java.util.Optional;

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
        // ✅ OAuth2 인증 정보 확인
        OAuth2AuthenticationToken authToken = getOAuth2AuthenticationToken().orElse(null);
        if (authToken == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "🚨 OAuth2 인증을 먼저 수행해야 합니다. 다시 로그인해주세요."
            ));
        }

        // ✅ OAuth2User 및 Provider 정보 가져오기
        OAuth2User user = authToken.getPrincipal();
        String provider = authToken.getAuthorizedClientRegistrationId();
        log.info("🔹 [OAuthController] OAuth 로그인 요청 - Provider: {}", provider);

        // ✅ OAuth 사용자 정보 처리
        UserDTO userDTO;
        try {
            userDTO = oAuthService.processOAuthUser(user, provider, authToken);
        } catch (Exception e) {
            log.error("🚨 [OAuthController] OAuth 사용자 정보 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "🚨 OAuth 사용자 정보 처리 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }

        // ✅ JWT 발급 및 저장
        try {
            String email = userDTO.getUserEmail();
            String existingRefreshToken = jwtTokenService.getRefreshToken(email);

            // ✅ 새로운 JWT 발급
            String accessToken = jwtTokenService.createAccessToken(email);
            String refreshToken = (existingRefreshToken != null) ? existingRefreshToken : jwtTokenService.createRefreshToken(email);

            // ✅ Redis에 JWT 저장 (Refresh Token은 없을 때만 저장)
            jwtTokenService.storeToken("accessToken:" + email, accessToken, 30 * 60 * 1000L); // 30분
            if (existingRefreshToken == null) {
                jwtTokenService.storeToken("refreshToken:" + email, refreshToken, 7 * 24 * 60 * 60 * 1000L); // 7일
            }

            log.info("✅ [OAuthController] JWT 발급 완료 - Email: {}, Access Token: [HIDDEN], Refresh Token: [HIDDEN]", email);

            // ✅ JSON 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tokens", Map.of(
                            "accessToken", accessToken,
                            "refreshToken", refreshToken,
                            "accessTokenExpiresIn", "30분",
                            "refreshTokenExpiresIn", "7일"
                    ),
                    "user", Map.of(
                            "email", email,
                            "name", userDTO.getUserName(),
                            "profileImage", userDTO.getProfileImage(),
                            "provider", provider
                    )
            ));
        } catch (Exception e) {
            log.error("🚨 [OAuthController] JWT 발급 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "🚨 JWT 발급 중 오류가 발생했습니다.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * ✅ SecurityContext에서 OAuth2AuthenticationToken 가져오는 메서드
     */
    private Optional<OAuth2AuthenticationToken> getOAuth2AuthenticationToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken authToken) {
            return Optional.of(authToken);
        }
        log.error("🚨 [OAuthController] SecurityContextHolder에 OAuth2 인증 정보가 없습니다!");
        return Optional.empty();
    }
}
