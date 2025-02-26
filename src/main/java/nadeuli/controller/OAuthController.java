/* OAuthController.java
 * 구글 및 카카오 OAuth 2.0 연동 - 인증
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.25        구글 및 카카오 통합 OAuth 2.0 인증 컨트롤러 구현
 * 국경민      2.26        로그인 성공 시 예외처리
 * ========================================================
 */

package nadeuli.controller;

import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User user, Model model) {
        Map<String, Object> attributes = user.getAttributes();
        String provider = (String) attributes.get("provider"); // 제공자 정보 획득
        String id = attributes.get("id") != null ? attributes.get("id").toString() : "Unknown";
        String email, nickname, profileImage;

        if ("google".equals(provider)) {
            // 구글 로그인 처리
            email = attributes.get("email") != null ? attributes.get("email").toString() : "Unknown";
            nickname = attributes.get("name") != null ? attributes.get("name").toString() : "Unknown";
            profileImage = attributes.get("picture") != null ? attributes.get("picture").toString() : "Unknown";
        } else {
            // 카카오 로그인 처리
            Map<String, String> properties = (Map<String, String>) attributes.get("properties");
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

            // properties 값이 null인 경우 기본값 설정
            nickname = properties != null && properties.get("nickname") != null ? properties.get("nickname") : "Unknown";
            profileImage = properties != null && properties.get("profile_image") != null ? properties.get("profile_image") : "Unknown";
            email = kakaoAccount != null && kakaoAccount.get("email") != null ? kakaoAccount.get("email").toString() : "Unknown";
        }

        // 사용자 정보 저장 또는 업데이트
        userRepository.findByEmail(email).ifPresentOrElse(
                existingUser -> {
                    if (!existingUser.getUserName().equals(nickname) ||
                            !existingUser.getProfileImage().equals(profileImage) ||
                            !existingUser.getId().equals(Long.parseLong(id))) {

                        User updatedUser = new User(email, nickname, profileImage, provider, existingUser.getRefreshToken());
                        userRepository.save(updatedUser);
                    }
                },
                () -> {
                    userRepository.save(new User(email, nickname, profileImage, provider, "YOUR_DEFAULT_REFRESH_TOKEN"));
                }
        );



        // 모델에 사용자 정보 추가
        model.addAttribute("nickname", nickname);
        model.addAttribute("profileImage", profileImage);
        model.addAttribute("email", email);

        return "hello";
    }
}