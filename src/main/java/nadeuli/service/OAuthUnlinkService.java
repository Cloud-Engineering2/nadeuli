/* OAuthUnlinkService.java
 * OAuth 계정 해제 및 회원 탈퇴 서비스
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04      OAuth 계정 해제 및 회원 삭제 서비스 초안
 *
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService; // ✅ Redis에서 JWT 삭제를 위해 사용
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_UNLINK_URL = "https://accounts.google.com/o/oauth2/revoke";

    /**
     * ✅ OAuth 계정 해제 및 회원 삭제
     */
    public boolean unlinkAndDeleteUser(String email) {
        if (!userRepository.existsByUserEmail(email)) {
            log.warn("🚨 [unlinkAndDeleteUser] 사용자 찾을 수 없음: {}", email);
            return false;
        }

        Optional<User> userOptional = userRepository.findByUserEmail(email);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        String provider = user.getProvider();
        String refreshToken = user.getRefreshToken();

        // ✅ OAuth 계정 해제 요청
        boolean isUnlinked = unlinkUser(provider, refreshToken);

        if (isUnlinked) {
            log.info("✅ [{}] OAuth 계정 해제 완료: {}", provider, email);

            // ✅ Redis에서 JWT 삭제
            jwtTokenService.deleteTokens("accessToken:" + email);
            jwtTokenService.deleteTokens("refreshToken:" + email);
            log.info("✅ [{}] Redis에서 JWT 삭제 완료", provider);

            // ✅ MySQL에서 사용자 삭제
            userRepository.deleteByUserEmail(email);
            log.info("✅ [{}] DB에서 사용자 삭제 완료: {}", provider, email);

            return true;
        }

        return false;
    }

    /**
     * ✅ OAuth 제공자별 계정 해제 API 호출
     */
    private boolean unlinkUser(String provider, String refreshToken) {
        return switch (provider) {
            case "kakao" -> unlinkKakaoUser(refreshToken);
            case "google" -> unlinkGoogleUser(refreshToken);
            default -> false;
        };
    }

    /**
     * ✅ 카카오 OAuth 계정 해제 API 호출
     */
    private boolean unlinkKakaoUser(String accessToken) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        var requestEntity = new org.springframework.http.HttpEntity<>(new org.springframework.util.LinkedMultiValueMap<>(), headers);
        var response = restTemplate.exchange(KAKAO_UNLINK_URL, org.springframework.http.HttpMethod.POST, requestEntity, String.class);

        return response.getStatusCode() == org.springframework.http.HttpStatus.OK;
    }

    /**
     * ✅ 구글 OAuth 계정 해제 API 호출
     */
    private boolean unlinkGoogleUser(String refreshToken) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        var request = new org.springframework.http.HttpEntity<>(new org.springframework.util.LinkedMultiValueMap<>(), headers);
        var response = restTemplate.exchange(GOOGLE_UNLINK_URL, org.springframework.http.HttpMethod.POST, request, String.class);

        return response.getStatusCode() == org.springframework.http.HttpStatus.OK;
    }
}

