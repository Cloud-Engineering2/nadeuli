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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final RefreshTokenService refreshTokenService;

    /**
     * ✅ OAuth 제공사에서 계정 연결 해제 (Google/Kakao) - 로그 추가 및 예외 처리 강화
     */
    public boolean unlinkProviderAccount(String email, String provider, String accessToken) {
        String unlinkUrl;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        if ("google".equals(provider)) {
            unlinkUrl = "https://accounts.google.com/o/oauth2/revoke?token=" + accessToken;
        } else if ("kakao".equals(provider)) {
            unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            requestEntity = new HttpEntity<>("target_id_type=user_id", headers);
        } else {
            log.warn("🚨 [unlinkProviderAccount] 지원되지 않는 OAuth 제공사: {}", provider);
            return false;
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(unlinkUrl, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ [unlinkProviderAccount] OAuth 계정 연결 해제 성공 - Email: {}, Provider: {}", email, provider);
                return true;
            } else {
                log.error("🚨 [unlinkProviderAccount] OAuth 해제 실패 - Email: {}, Provider: {}, 응답 코드: {}", email, provider, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("🚨 [unlinkProviderAccount] OAuth 계정 연결 해제 중 오류 발생 - Email: {}, Provider: {}, 오류: {}", email, provider, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ OAuth 계정 해제 및 사용자 정보 삭제 (로그 추가 및 예외 처리 강화)
     */
    @Transactional
    public boolean unlinkAndDeleteUser(String email, String accessToken) {
        User user = userRepository.findByUserEmail(email).orElse(null);

        if (user == null) {
            log.warn("🚨 [unlinkAndDeleteUser] 사용자 정보를 찾을 수 없음 - Email: {}", email);
            return false;
        }

        // OAuth 계정 해제
        boolean unlinkSuccess = unlinkProviderAccount(email, user.getProvider(), accessToken);
        if (!unlinkSuccess) {
            log.error("🚨 [unlinkAndDeleteUser] OAuth 계정 해제 실패 - Email: {}", email);
            return false;
        }

        // Refresh Token 삭제 (회원 탈퇴 시만 삭제)
        refreshTokenService.deleteRefreshToken(email);

        // 사용자 정보 삭제
        userRepository.delete(user);
        log.info("✅ [unlinkAndDeleteUser] 사용자 정보 삭제 완료 - Email: {}", email);
        return true;
    }
}