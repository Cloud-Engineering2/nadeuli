package nadeuli.controller;

/* KakaoOAuthController.java
 * 카카오 OAuth 2.0 연동 - 인증
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 김대환       2.25      Entity 변경에 따른 코드 수정
 *
 * ========================================================
 */

import nadeuli.entity.KakaoUser;
import nadeuli.repository.KakaoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class KakaoOAuthController {

    private final KakaoUserRepository kakaoUserRepository;

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User user, Model model) {
        Map<String, Object> attributes = user.getAttributes();
        Long uid = Long.parseLong(attributes.get("id").toString());
        Map<String, String> properties = (Map<String, String>) attributes.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        String nickname = properties.get("nickname");
        String profileImage = properties.get("profile_image");
        String email = kakaoAccount.get("email").toString();

        kakaoUserRepository.findByUid(uid).ifPresentOrElse(
                existingUser -> {
                    if (!existingUser.getUser_name().equals(nickname) || !existingUser.getImage_url().equals(profileImage)) {
                        KakaoUser updatedUser = new KakaoUser(uid, email, "", nickname, "kakao", profileImage, "ROLE_USER");
                        kakaoUserRepository.save(updatedUser);
                    }
                },
                () -> {
                    kakaoUserRepository.save(new KakaoUser(uid, email, "", nickname, "kakao", profileImage, "ROLE_USER"));
                }
        );

        model.addAttribute("nickname", nickname);
        model.addAttribute("profileImage", profileImage);
        model.addAttribute("email", email);

        return "hello";
    }
}

