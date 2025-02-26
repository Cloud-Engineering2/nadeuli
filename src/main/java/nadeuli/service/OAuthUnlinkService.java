/* OAuthUnlinkService.java
 * 구글 및 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.25        구글 및 카카오 통합 OAuth 2.0 언링크 서비스 구현
 *
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthUnlinkService {

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    // Google Access Token을 얻기 위한 메서드
    private String getGoogleAccessToken() {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    // Google Access Token을 갱신하기 위한 메서드
    private String getRefreshedGoogleAccessToken(String refreshToken) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    // Kakao와 Google Unlink URL 상수
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final String GOOGLE_UNLINK_URL = "https://accounts.google.com/o/oauth2/revoke";

    // RestTemplate 인스턴스 생성
    private final RestTemplate restTemplate = new RestTemplate();

    // 사용자 연결 해제 메서드
    public boolean unlinkUser(Long userId, String provider, String refreshToken) {
        if (userId == null || provider == null) {
            System.err.println("오류: userId 또는 provider 값이 null입니다.");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        String unlinkUrl;
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        if ("kakao".equals(provider)) {
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            unlinkUrl = KAKAO_UNLINK_URL;
            body.add("target_id_type", "user_id");
            body.add("target_id", String.valueOf(userId));
        } else if ("google".equals(provider)) {
            // Access Token 갱신
            String accessToken = getRefreshedGoogleAccessToken(refreshToken);
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            unlinkUrl = GOOGLE_UNLINK_URL;
            body.add("token", String.valueOf(userId));
        } else {
            System.err.println("오류: 지원하지 않는 provider입니다. → " + provider);
            return false;
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    unlinkUrl, HttpMethod.POST, requestEntity, String.class
            );

            System.out.println(provider + " 연결 해제 API 응답: " + response.getBody());
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println(provider + " 연결 해제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
