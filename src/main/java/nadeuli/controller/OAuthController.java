package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final Environment env;

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_UNLINK_URL = "https://oauth2.googleapis.com/revoke?token=";

    /** 🔹 로그아웃 API */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = "accessToken", required = false) String accessTokenFromCookie) {

        log.info("로그아웃 요청 - Authorization Header: {}", authHeader);

        String jwtAccessToken = extractToken(authHeader, accessTokenFromCookie);

        if (jwtAccessToken == null) {
            log.warn("로그아웃 요청 - 인증 정보 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인된 사용자가 없습니다."
            ));
        }

        ResponseCookie expiredAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .build();

        log.info("로그아웃 성공");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessTokenCookie.toString())
                .body(Map.of(
                        "success", true,
                        "message", "로그아웃 되었습니다."
                ));
    }

    /** 🔹 OAuth 계정 연결 해제 API */
    @DeleteMapping("/unlink/{email}")
    public ResponseEntity<Map<String, Object>> unlinkUser(@PathVariable String email) {
        Optional<User> userOptional = userRepository.findByUserEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("[OAuthUnlink] 사용자를 찾을 수 없음: {}", email);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "해당 이메일의 사용자를 찾을 수 없습니다."
            ));
        }

        User user = userOptional.get();
        String provider = user.getProvider();
        String accessToken = user.getUserToken();

        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("[OAuthUnlink] 저장된 Access Token 없음 - 이메일: {}", email);
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "OAuth Access Token이 없습니다. 다시 로그인 후 시도해주세요."
            ));
        }

        boolean unlinkSuccess = unlinkUserByProvider(provider, accessToken);

        if (!unlinkSuccess) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "OAuth 계정 해제 실패"
            ));
        }

        userRepository.delete(user);
        log.info("[{}] OAuth 계정 해제 및 사용자 삭제 완료 - Email: {}", provider, email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OAuth 계정 해제 및 사용자 삭제 완료"
        ));
    }

    /** 🔹 OAuth 제공자별 계정 해제 처리 */
    private boolean unlinkUserByProvider(String provider, String accessToken) {
        switch (provider.toLowerCase()) {
            case "kakao":
                return unlinkKakaoUser(accessToken);
            case "google":
                return unlinkGoogleUser(accessToken);
            default:
                log.error("[OAuthUnlink] 지원되지 않는 OAuth 제공자: {}", provider);
                return false;
        }
    }

    /** 🔹 카카오 계정 연결 해제 */
    private boolean unlinkKakaoUser(String accessToken) {
        return sendUnlinkRequest(KAKAO_UNLINK_URL, accessToken, HttpMethod.POST);
    }

    /** 🔹 구글 계정 연결 해제 */
    private boolean unlinkGoogleUser(String accessToken) {
        return sendUnlinkRequest(GOOGLE_UNLINK_URL + accessToken, accessToken, HttpMethod.POST);
    }

    /** 🔹 OAuth 해제 요청 공통 처리 */
    private boolean sendUnlinkRequest(String url, String accessToken, HttpMethod method) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(url, method, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[OAuth] 계정 해제 성공 - URL: {}", url);
                return true;
            }
        } catch (Exception e) {
            log.error("[OAuth] 계정 해제 중 오류 발생 - URL: {}, 에러: {}", url, e.getMessage());
        }
        return false;
    }

    /** 🔹 Authorization 헤더 또는 쿠키에서 토큰 추출 */
    private String extractToken(String authHeader, String cookieToken) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "").trim();
        } else if (cookieToken != null) {
            return cookieToken;
        }
        return null;
    }
}
