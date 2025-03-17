package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth2")
public class OAuthTokenController {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> generateJwtToken() {
        // ✅ 현재 SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("🔍 SecurityContext 인증 정보: {}", authentication);

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

        // 🔹 기존 사용자 확인 (없으면 새로 생성)
        Optional<User> existingUser = userRepository.findByUserEmail(email);
        if (existingUser.isEmpty()) {
            log.info("🆕 신규 OAuth 사용자 등록: {}", email);
            User newUser = User.createNewUser(email, oidcUser.getAttribute("name"),
                    oidcUser.getAttribute("picture"), "google",
                    oidcUser.getAttribute("sub"), null, null, null);
            userRepository.save(newUser);
        }

        // 🔹 JWT 토큰 발급
        String accessToken = jwtTokenService.generateAccessToken(email);
        String refreshToken = jwtTokenService.generateRefreshToken(email).token;

        log.info("✅ JWT 발급 완료 - AccessToken: {}, RefreshToken: {}", accessToken, refreshToken);

        // 🔹 프론트엔드에서 사용할 JSON 응답
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }
}
