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
                    "message", "저장된 Access Token이 없습니다. 다시 로그인 후 시도해주세요."
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
