package nadeuli.service;

/* KakaoUnlinkService.java
 * 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class KakaoUnlinkService {

    @Value("${kakao.admin-key}")
    private String adminKey;

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean unlinkUser(Long userId) {
        if (userId == null) {
            System.err.println("오류: userId 값이 null입니다.");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", String.valueOf(userId));

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_UNLINK_URL, HttpMethod.POST, requestEntity, String.class
            );

            System.out.println("카카오 연결 해제 API 응답: " + response.getBody());
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            System.err.println("카카오 연결 해제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
