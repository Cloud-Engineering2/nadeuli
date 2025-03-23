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
 * 국경민          2025.03.20     회원 탈퇴 시 uid 기반으로 변경
 * ========================================================
 */


package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.S3Service;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


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
    private final S3Service s3Service;

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_UNLINK_URL = "https://oauth2.googleapis.com/revoke?token=";
    private static final String GOOGLE_TOKEN_URL = ""; // 비어있음

    @DeleteMapping("/unlink")
    public ResponseEntity<Map<String, Object>> unlinkUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[OAuthUnlink] 요청 거부 - 로그인되지 않은 사용자");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 1️⃣ SecurityContextHolder에서 현재 로그인된 사용자의 이메일 가져오기
        String email = authentication.getName(); // 현재 사용자의 이메일

        // 2️⃣ 이메일을 기반으로 User 엔티티 조회
        Optional<User> userOptional = userRepository.findByUserEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("[OAuthUnlink] 사용자 찾기 실패 - email: {}", email);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "사용자를 찾을 수 없습니다."
            ));
        }

        User user = userOptional.get();
        Long id = user.getId(); // UID 가져오기
        String provider = user.getProvider();
        String accessToken = user.getProviderRefreshToken();
//        String refreshToken = user.getRefreshToken(); // ✅ 구글 재발급용

        log.info("[OAuthUnlink] 회원 탈퇴 요청 - UID: {}, Email: {}", id, email);

        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("[OAuthUnlink] 저장된 Access Token 없음 - UID: {}, provider: {}", id, provider);
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "OAuth Access Token이 없습니다. 다시 로그인 후 시도해주세요."
            ));
        }

        // 3️⃣ OAuth 계정 해제 요청
        boolean unlinkSuccess = switch (provider.toLowerCase()) {
            case "kakao" -> unlinkKakaoUser(accessToken);
            case "google" -> {
                boolean success = unlinkGoogleUser(accessToken);
//                if (!success && refreshToken != null && !refreshToken.isEmpty()) {
//                    String newAccessToken = refreshGoogleAccessToken(refreshToken);
//                    if (newAccessToken != null) {
//                        log.info("[Google] access_token 재발급 성공 → unlink 재시도");
//                        success = unlinkGoogleUser(newAccessToken);
//                    } else {
//                        log.warn("[Google] access_token 재발급 실패 → unlink 불가");
//                    }
//                }
                yield success;
            }
            default -> {
                log.error("[OAuthUnlink] 지원되지 않는 OAuth 제공자 - UID: {}, provider: {}", id, provider);
                yield false;
            }
        };

        if (!unlinkSuccess) {
            log.error("[OAuthUnlink] OAuth 계정 해제 실패 - UID: {}, provider: {}", id, provider);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "OAuth 계정 해제 실패"
            ));
        }

        // 4️⃣ 사용자 삭제 전 - S3 프로필 이미지 삭제
        String profileImageUrl = user.getProfileImage();
        if (profileImageUrl != null && s3Service.isS3Image(profileImageUrl)) {
            log.info("[OAuthUnlink] S3 이미지 삭제 시도: {}", profileImageUrl);
            try {
                s3Service.deleteFile(profileImageUrl);
                log.info("[OAuthUnlink] S3 이미지 삭제 완료");
            } catch (Exception e) {
                log.warn("[OAuthUnlink] S3 이미지 삭제 실패: {}", e.getMessage());
            }
        }

        // 5️⃣  사용자 삭제
        userRepository.delete(user);
        log.info("[OAuthUnlink] OAuth 계정 해제 및 사용자 삭제 완료 - UID: {}, provider: {}", id, provider);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", provider + " 계정 해제 및 사용자 삭제 완료"
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

    // ✅ 구글 refresh_token 으로 access_token 재발급
//    private String refreshGoogleAccessToken(String refreshToken) {
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            String body = "client_id=" + env.getProperty("oauth.google.client-id")
//                    + "&client_secret=" + env.getProperty("oauth.google.client-secret")
//                    + "&refresh_token=" + refreshToken
//                    + "&grant_type=refresh_token";
//
//            HttpEntity<String> request = new HttpEntity<>(body, headers);
//            ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                return (String) response.getBody().get("access_token");
//            }
//        } catch (Exception e) {
//            log.error("[Google] access_token 재발급 실패: {}", e.getMessage());
//        }
//        return null;
//    }

}
