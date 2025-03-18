package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth2")
public class OAuthTokenController {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    /**
     * 🔹 OAuth 로그인 후 JWT 발급
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateJwtToken(@RequestBody Map<String, String> requestBody) {
        String authorizationCode = requestBody.get("authorizationCode");
        log.info("🔍 Received Authorization Code: {}", authorizationCode);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser)) {
            log.error("🚨 OAuth 사용자 정보 없음 (SecurityContext 비어있음)");
            return ResponseEntity.badRequest().build();
        }

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getAttribute("email");
        if (email == null) {
            log.error("🚨 OAuth 사용자 이메일 없음");
            return ResponseEntity.badRequest().build();
        }

        log.info("✅ OAuth 사용자 확인됨: {}", email);
        Optional<User> existingUser = userRepository.findByUserEmail(email);
        if (existingUser.isEmpty()) {
            log.info("🆕 신규 OAuth 사용자 등록: {}", email);
            User newUser = User.createNewUser(email, oidcUser.getAttribute("name"),
                    oidcUser.getAttribute("picture"), "google",
                    oidcUser.getAttribute("sub"), null, null, null);
            userRepository.save(newUser);
        }

        String accessToken = jwtTokenService.generateAccessToken(email);
        String refreshToken = jwtTokenService.generateRefreshToken(email).token;

        log.info("✅ JWT 발급 완료 - AccessToken: {}, RefreshToken: {}", accessToken, refreshToken);

        return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
    }

    /**
     * 🔹 액세스 토큰 갱신 (리프레시 토큰을 이용)
     */
    @PostMapping("/refresh/access")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtTokenService.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid or expired Refresh Token");
        }

        String email = jwtTokenService.extractEmail(refreshToken);
        User user = userRepository.findByUserEmail(email).orElseThrow();
        if (!refreshToken.equals(user.getRefreshToken())) {
            return ResponseEntity.status(401).body("Token mismatch");
        }

        String newAccessToken = jwtTokenService.generateAccessToken(email);
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString()).body(Map.of("message", "Access Token 갱신 완료"));
    }
}
