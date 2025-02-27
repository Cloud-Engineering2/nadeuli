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
 * 국경민      2.27        JWT 토큰 생성 및 로그인 성공 후 반환
 * 국경민      2.27        Redis를 사용한 액세스 토큰 및 리프레시 토큰 관리
 * ========================================================
 */

package nadeuli.controller;

import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/loginSuccess")
    public RedirectView loginSuccess(@AuthenticationPrincipal OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        String provider = user.getAttribute("provider");

        String email = "Unknown";
        String nickname = "Unknown";
        String profileImage = "defaultImage";

        if ("google".equals(provider)) {
            // 구글 로그인 처리
            email = user.getAttribute("email");
            nickname = user.getAttribute("name");
            profileImage = user.getAttribute("picture") != null ? user.getAttribute("picture") : "defaultImage";
        } else if ("kakao".equals(provider)) {
            // 카카오 로그인 처리
            Map<String, Object> properties = user.getAttribute("properties");
            Map<String, Object> kakaoAccount = user.getAttribute("kakao_account");

            // properties 값이 null인 경우 기본값 설정
            email = kakaoAccount != null ? (String) kakaoAccount.get("email") : "Unknown";
            nickname = properties != null ? (String) properties.get("nickname") : "Unknown";
            profileImage = properties != null ? (String) properties.get("profile_image") : "defaultImage";
        }

        final String finalEmail = email;
        final String finalNickname = nickname;
        final String finalProfileImage = profileImage;

        // 사용자 정보 저장 또는 업데이트
        userRepository.findByEmail(email).ifPresentOrElse(
                existingUser -> {
                    // 기존 사용자 업데이트
                    User updatedUser = new User(
                            existingUser.getId(),
                            finalEmail,
                            finalNickname,
                            finalProfileImage,
                            provider,
                            existingUser.getRefreshToken()
                    );
                    userRepository.save(updatedUser);
                },
                () -> {
                    // 새로운 사용자 생성
                    User newUser = new User(
                            null,
                            finalEmail,
                            finalNickname,
                            finalProfileImage,
                            provider,
                            "defaultToken"
                    );
                    userRepository.save(newUser);
                }
        );

        // 액세스 토큰 및 리프레시 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // Redis에 토큰 저장
        jwtTokenProvider.storeToken("accessToken:" + email, accessToken, 30 * 60 * 1000L); // 30분
        jwtTokenProvider.storeToken("refreshToken:" + email, refreshToken, 7 * 24 * 60 * 60 * 1000L); // 1주일

        // JWT 액세스 토큰을 리디렉션 URL에 포함하여 반환
        return new RedirectView("/profile?token=" + accessToken);
    }
}


