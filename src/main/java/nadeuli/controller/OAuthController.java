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
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.UserDTO;
import nadeuli.service.JwtTokenService;
import nadeuli.service.OAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthController {
    private final OAuthService oAuthService;
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ OAuth 로그인 성공 시 호출되는 엔드포인트
     * - OAuth 사용자 정보를 가져와 DB에 저장
     * - JWT 토큰 발급 후 Redis에 저장
     */
    @GetMapping("/loginSuccess")
    public ResponseEntity<Map<String, String>> loginSuccess(
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam("registrationId") String provider) { // ✅ provider 추가

        // 1️⃣ provider 값이 올바르게 전달되었는지 검증
        if (provider == null || provider.isEmpty()) {
            throw new IllegalStateException("🚨 provider 값이 설정되지 않았습니다!");
        }

        // 2️⃣ OAuth 사용자 정보 처리
        UserDTO userDTO = oAuthService.processOAuthUser(user, provider); // ✅ provider 값 전달

        // 3️⃣ JWT 액세스 및 리프레시 토큰 발급
        String accessToken = jwtTokenService.createAccessToken(userDTO.getUserEmail());
        String refreshToken = jwtTokenService.createRefreshToken(userDTO.getUserEmail());

        // 4️⃣ Redis에 토큰 저장
        jwtTokenService.storeToken("accessToken:" + userDTO.getUserEmail(), accessToken, 30 * 60 * 1000L); // ✅ 30분
        jwtTokenService.storeToken("refreshToken:" + userDTO.getUserEmail(), refreshToken, 7 * 24 * 60 * 60 * 1000L); // ✅ 1주일

        // 5️⃣ 프론트엔드에 응답 반환
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userEmail", userDTO.getUserEmail(),
                "userName", userDTO.getUserName(),
                "profileImage", userDTO.getProfileImage(),
                "provider", userDTO.getProvider()
        ));
    }
}
