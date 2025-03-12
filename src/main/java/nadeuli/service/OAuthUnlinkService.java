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
 * 국경민      03-04       OAuth 계정 해제 및 회원 삭제 서비스 초안
 * 국경민      03-05       Redis에서 JWT 삭제 추가 및 코드 최적화
 * 국경민      03-05       OAuth 제공자별 계정 해제 API 요청 추가 및 예외 처리 강화
 * 국경민      03-12       불필요한 Optional 처리 제거 및 보안 강화
 * 국경민      03-12       향상된 Switch 문 적용하여 가독성 개선
 * 국경민      03-12       `@Value` 대신 `Environment` 사용하여 환경 변수 처리 개선
 * 국경민      03-12       RestTemplate을 올바르게 주입받도록 변경 및 예외 처리 강화
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService; // ✅ Redis에서 JWT 삭제를 위해 사용
    private final RestTemplate restTemplate; // ✅ Bean으로 등록한 RestTemplate 주입
    private final Environment env; // ✅ 환경 변수에서 값 가져오기 위해 사용

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_UNLINK_URL = "https://oauth2.googleapis.com/revoke?token=";

    /**
     * ✅ OAuth 계정 해제 및 회원 삭제
     */
    public boolean unlinkAndDeleteUser(String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("🚨 [unlinkAndDeleteUser] 사용자 찾을 수 없음: " + email));

        String provider = user.getProvider();
        String refreshToken = user.getRefreshToken(); // ✅ Refresh Token 사용

        // ✅ OAuth 계정 해제 실패 시 삭제 진행하지 않음
        if (!unlinkUser(provider, refreshToken)) {
            log.warn("🚨 [{}] OAuth 계정 해제 실패 - 사용자 삭제 중단: {}", provider, email);
            return false;
        }

        log.info("✅ [{}] OAuth 계정 해제 완료: {}", provider, email);

        // ✅ Redis에서 JWT 삭제
        boolean accessDeleted = jwtTokenService.deleteTokens("accessToken:" + email);
        boolean refreshDeleted = jwtTokenService.deleteTokens("refreshToken:" + email);
        log.info("✅ [{}] Redis에서 JWT 삭제 완료 - AccessToken 삭제: {}, RefreshToken 삭제: {}", provider, accessDeleted, refreshDeleted);

        // ✅ MySQL에서 사용자 삭제
        try {
            userRepository.delete(user);
            log.info("✅ [{}] DB에서 사용자 삭제 완료: {}", provider, email);
            return true;
        } catch (Exception e) {
            log.error("🚨 [{}] 사용자 삭제 중 오류 발생: {}", provider, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ OAuth 제공자별 계정 해제 API 호출 (향상된 switch 적용)
     */
    private boolean unlinkUser(String provider, String refreshToken) {
        return switch (provider.toLowerCase()) {
            case "kakao" -> unlinkKakaoUser();
            case "google" -> unlinkGoogleUser(refreshToken);
            default -> throw new IllegalArgumentException("🚨 지원되지 않는 OAuth 제공자: " + provider);
        };
    }

    /**
     * ✅ 카카오 OAuth 계정 해제 API 호출
     */
    private boolean unlinkKakaoUser() {
        try {
            String kakaoAdminKey = env.getProperty("kakao.admin-key"); // ✅ `@Value` 대신 `Environment` 사용

            if (kakaoAdminKey == null || kakaoAdminKey.isBlank()) {
                log.error("🚨 [Kakao] 카카오 Admin Key가 설정되지 않음.");
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(KAKAO_UNLINK_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ [Kakao] 계정 해제 성공");
                return true;
            }
        } catch (Exception e) {
            log.error("🚨 [Kakao] 계정 해제 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }

    /**
     * ✅ 구글 OAuth 계정 해제 API 호출
     */
    private boolean unlinkGoogleUser(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("🚨 [Google] Refresh Token이 없음. 계정 해제 불가");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(GOOGLE_UNLINK_URL + refreshToken, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ [Google] 계정 해제 성공");
                return true;
            }
        } catch (Exception e) {
            log.error("🚨 [Google] 계정 해제 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }
}
