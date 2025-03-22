/*
 * OAuthController.java
 * OAuth 인증 사용자 로그아웃 및 계정 연동 해제 처리 컨트롤러
 * - JWT Access/Refresh Token 삭제 처리 (쿠키 기반)
 * - Kakao / Google OAuth 계정 연동 해제(탈퇴) 요청 처리
 *
 * 작성자 : 국경민, 김대환
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 국경민, 김대환   2025.03.19     최초 작성 - OAuth 로그아웃 및 계정 연동 해제 처리 구현
 * ========================================================
 */


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

//
//@PostMapping("/logout")
//public ResponseEntity<Map<String, Object>> logout(
//        @RequestHeader(value = "Authorization", required = false) String authHeader,
//        @CookieValue(name = "accessToken", required = false) String accessTokenFromCookie,
//        @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie) {
//
//    log.info("로그아웃 요청 - Authorization Header: {}", authHeader);
//
//    String jwtAccessToken = null;
//    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        jwtAccessToken = authHeader.replace("Bearer ", "").trim();
//    } else if (accessTokenFromCookie != null) {
//        jwtAccessToken = accessTokenFromCookie;
//    }
//
//    if (jwtAccessToken == null) {
//        log.warn("로그아웃 요청 - 인증 정보 없음");
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
//                "success", false,
//                "message", "로그인된 사용자가 없습니다."
//        ));
//    }
//
//    ResponseCookie expiredAccessTokenCookie = ResponseCookie.from("accessToken", "")
//            .path("/")
//            .maxAge(0)
//            .httpOnly(true)
//            .secure(true) // ✅ 로그인 시 secure=true였다면 반드시 필요
//            .build();
//
//    ResponseCookie expiredRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
//            .path("/")
//            .maxAge(0)
//            .httpOnly(true)
//            .secure(true) // ✅ 로그인 시 secure=true였다면 반드시 필요
//            .build();
//
//    log.info("로그아웃 성공 - accessToken, refreshToken 삭제 완료");
//
//    return ResponseEntity.ok()
//            .header(HttpHeaders.SET_COOKIE, expiredAccessTokenCookie.toString())
//            .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString()) // 이렇게 이어서!
//            .body(Map.of(
//                    "success", true,
//                    "message", "로그아웃 완료"
//            ));
//}



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

        boolean unlinkSuccess = switch (provider.toLowerCase()) {
            case "kakao" -> unlinkKakaoUser(accessToken);
            case "google" -> unlinkGoogleUser(accessToken);
            default -> {
                log.error("[OAuthUnlink] 지원되지 않는 OAuth 제공자: {}", provider);
                yield false;
            }
        };

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

    private boolean unlinkKakaoUser(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(KAKAO_UNLINK_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[Kakao] 계정 해제 성공");
                return true;
            }
        } catch (Exception e) {
            log.error("[Kakao] 계정 해제 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }

    private boolean unlinkGoogleUser(String accessToken) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_UNLINK_URL + accessToken, null, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("[Google] 계정 해제 성공");
                return true;
            }
        } catch (Exception e) {
            log.error("[Google] 계정 해제 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }
}