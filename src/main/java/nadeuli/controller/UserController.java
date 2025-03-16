package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/auth/success")
    public ResponseEntity<?> oauthSuccess(OAuth2AuthenticationToken authToken) {
        String email = authToken.getPrincipal().getAttribute("email");
        User user = userRepository.findByUserEmail(email).orElseThrow();

        String accessToken = jwtTokenService.generateAccessToken(email);
        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);

        user.updateRefreshToken(refreshTokenResponse.token, refreshTokenResponse.expiryAt);
        userRepository.save(user);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(900)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString()) // Access Token 쿠키에 저장
                .body(Map.of("message", "로그인"));
    }



    @PostMapping("/refresh/access")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        log.info("refreshToken: {}", refreshToken);

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("refreshToken이 비어 있음 쿠키가 전달되지 않음.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired Refresh Token");
        }

        if (!jwtTokenService.validateToken(refreshToken)) {
            log.warn("JWT 검증 실패: {}", refreshToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired Refresh Token");
        }

        String email = jwtTokenService.extractEmail(refreshToken);
        User user = userRepository.findByUserEmail(email).orElseThrow();

        if (!refreshToken.equals(user.getRefreshToken())) {
            log.warn("Token mismatch! 요청된 refreshToken: {}, 저장된 refreshToken: {}", refreshToken, user.getRefreshToken());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mismatch");
        }

        String newAccessToken = jwtTokenService.generateAccessToken(email);
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(Map.of("message", "니가 만든 작은 쿠키."));
    }


    @PostMapping("/refresh/refresh")
    public ResponseEntity<?> refreshTokens(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtTokenService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("쿠키 만료");
        }

        String email = jwtTokenService.extractEmail(refreshToken);
        User user = userRepository.findByUserEmail(email).orElseThrow();

        if (!refreshToken.equals(user.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("쿠키 미스 매치");
        }

        String newAccessToken = jwtTokenService.generateAccessToken(email);
        JwtTokenService.TokenResponse newRefreshTokenResponse = jwtTokenService.generateRefreshToken(email);

        user.updateRefreshToken(newRefreshTokenResponse.token, newRefreshTokenResponse.expiryAt);
        userRepository.save(user);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", newRefreshTokenResponse.token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(Map.of("message", "니가 만든 큰 쿠키"));
    }

    @GetMapping("/user/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("인증 정보 없음, 최근 로그인한 사용자 조회");

            User user = userRepository.findFirstByOrderByLastLoginAtDesc()
                    .orElseGet(() -> userRepository.findFirstByOrderByCreatedAtDesc().orElse(null));

            if (user == null) {
                log.warn("사용자가 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            return ResponseEntity.ok(UserDTO.from(user));
        }

        log.info("SecurityContext Authentication: {}", authentication);
        User user = null;

        if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
            Object principal = authToken.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                user = ((CustomUserDetails) principal).getUser();
                log.info("CustomUserDetails에서 User 추출: {}", user.getUserEmail());
            }
        } else if (authentication instanceof OAuth2AuthenticationToken authToken) {
            Map<String, Object> attributes = authToken.getPrincipal().getAttributes();
            String email = extractEmailFromAttributes(attributes);

            log.info("OAuth2AuthenticationToken 이메일: {}", email);

            if (email == null || email.isEmpty()) {
                log.warn("이메일 정보가 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            user = userRepository.findByUserEmail(email).orElse(null);
            if (user == null) {
                log.warn("DB에서 사용자 정보를 찾을 수 없음 - 이메일: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }

        if (user == null) {
            log.warn("인증된 사용자 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        return ResponseEntity.ok(UserDTO.from(user));
    }

    private String extractEmailFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("email")) {
            return attributes.get("email").toString();
        }
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount.containsKey("email")) {
                return kakaoAccount.get("email").toString();
            }
        }
        return null;
    }
}