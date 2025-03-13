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
import nadeuli.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    /**
     * ✅ OAuth 로그인 성공 후 JWT 발급 API
     */
    @GetMapping("/loginSuccess")
    public ResponseEntity<Map<String, Object>> loginSuccess() {
        // ✅ SecurityContext에서 OAuth2 인증 정보 가져오기
        OAuth2AuthenticationToken authToken = getOAuth2AuthenticationToken().orElse(null);
        if (authToken == null) {
            log.warn("🚨 [OAuthController] OAuth2 인증 정보를 찾을 수 없습니다!");
            return buildErrorResponse("🚨 OAuth2 인증을 먼저 수행해야 합니다. 다시 로그인해주세요.");
        }

        log.info("✅ [OAuthController] SecurityContext 인증 정보 확인 - Principal: {}", authToken.getPrincipal());

        OAuth2User user = authToken.getPrincipal();
        String provider = authToken.getAuthorizedClientRegistrationId();
        log.info("🔹 [OAuthController] OAuth 로그인 요청 - Provider: {}", provider);

        // ✅ OAuth 사용자 정보 처리
        UserDTO userDTO = oAuthService.processOAuthUser(user, provider);

        // ✅ SecurityContextHolder에 인증 정보 강제 저장 (유지 문제 해결)
        forceSaveAuthentication(userDTO, authToken);

        // ✅ JWT 발급
        String accessToken = jwtTokenService.createAccessToken(userDTO.getUserEmail());
        String refreshToken = refreshTokenService.getOrGenerateRefreshToken(userDTO.getUserEmail(), provider);
        log.info("✅ [OAuthController] JWT 발급 완료 - Email: {}", userDTO.getUserEmail());

        return buildSuccessResponse(userDTO, accessToken, refreshToken);
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

    /**
     * ✅ SecurityContextHolder에 인증 정보 강제 저장 (OAuth2 로그인 유지 문제 해결)
     */
    private void forceSaveAuthentication(UserDTO userDTO, OAuth2AuthenticationToken authToken) {
        log.info("✅ [OAuthController] SecurityContextHolder에 인증 정보 강제 저장 시작");

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                userDTO, null, authToken.getAuthorities()
        ));
        SecurityContextHolder.setContext(securityContext);

        log.info("✅ [OAuthController] SecurityContextHolder에 사용자 정보 저장 완료 - Email: {}", userDTO.getUserEmail());
    }

    /**
     * ✅ JSON 성공 응답 반환
     */
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(UserDTO userDTO, String accessToken, String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        response.put("tokens", tokens);
        response.put("user", userDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ JSON 에러 응답 반환
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        log.warn("🚨 [OAuthController] 오류 응답 반환: {}", message);
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", message
        ));
    }
}
