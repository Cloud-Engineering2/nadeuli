package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("인증 정보 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        log.info("현재 SecurityContext Authentication: {}", authentication);

        User user = null;
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            user = (User) authentication.getPrincipal();
            log.info("UsernamePasswordAuthenticationToken 사용자 정보: {}", user.getUserEmail());
        } else if (authentication instanceof OAuth2AuthenticationToken authToken) {
            Map<String, Object> attributes = authToken.getPrincipal().getAttributes();
            String email = extractEmailFromAttributes(attributes);

            log.info("OAuth2AuthenticationToken 이메일: {}", email);

            if (email == null || email.isEmpty()) {
                log.warn("🚨 이메일 정보가 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            user = userRepository.findByUserEmail(email).orElse(null);
            if (user == null) {
                log.warn("DB에서 사용자 정보를 찾을 수 없음 - 이메일: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }

        if (user == null) {
            log.warn("사용자 정보 없음");
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
