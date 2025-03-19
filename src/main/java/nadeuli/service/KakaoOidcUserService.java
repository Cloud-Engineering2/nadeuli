/*
 * KakaoOidcUserService.java
 * Kakao OAuth2 로그인 인증 처리 및 사용자 정보 관리 서비스
 * - 사용자 정보를 기반으로 회원 등록 또는 업데이트 처리
 * - JWT Access/Refresh Token 생성 및 사용자 정보에 저장
 * - 카카오 계정에서 email, nickname, profile_image_url 추출 처리 포함
 *
 * 작성자 : 국경민, 김대환
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 국경민, 김대환   2025.03.19     최초 작성 - Kakao OAuth 로그인 처리 및 JWT 토큰 발급 로직 구현
 * 김대환 2025.03.19 User_Role 기본값 지정
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOidcUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if (!"kakao".equals(provider)) {
            log.warn("[KakaoOidcUserService] Unsupported provider '{}'", provider);
            return oAuth2User;
        }

        String kakaoAccessToken = userRequest.getAccessToken().getTokenValue();
        log.info("[Kakao OAuth] 카카오 Access Token: {}", kakaoAccessToken);

        return processOAuthUser(oAuth2User, provider, kakaoAccessToken);
    }

    public OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String kakaoAccessToken) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmailFromAttributes(attributes);
        String name = extractUserNameFromAttributes(attributes);
        String profileImage = extractProfileImageFromAttributes(attributes);

        log.info("[{} OAuth] Email: {}, Name: {}, ProfileImage: {}", provider, email, name, profileImage);

        User userEntity = userRepository.findByUserEmail(email)
                .map(existing -> updateExistingUser(existing, name, profileImage, kakaoAccessToken))
                .orElseGet(() -> {
                    User newUser = createNewUser(email, name, profileImage, kakaoAccessToken);
                    newUser.setUserRole(UserRole.MEMBER);
                    return newUser;
                });

        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        userEntity.updateRefreshToken(refreshTokenResponse.token, refreshTokenResponse.expiryAt);
        userRepository.save(userEntity);

        String jwtAccessToken = jwtTokenService.generateAccessToken(email);
        log.info("[{} OAuth] JWT Access Token 발급 완료: {}", provider, jwtAccessToken);
        log.info("[{} OAuth] JWT Refresh Token 발급 완료: {}", provider, refreshTokenResponse.token);

        log.info("[{} OAuth] 저장된 Kakao Access Token (user_token): {}", provider, userEntity.getUserToken());

        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("accessToken", jwtAccessToken);
        updatedAttributes.put("email", email);

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                updatedAttributes,
                "email"
        );
    }

    private User updateExistingUser(User user, String name, String profileImage, String kakaoAccessToken) {
        log.info("기존 사용자 정보 업데이트 - Email: {}, AccessToken 변경 여부: {}", user.getUserEmail(), !kakaoAccessToken.equals(user.getUserToken()));
        user.updateProfile(name, profileImage, "kakao", kakaoAccessToken, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String kakaoAccessToken) {
        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "kakao", kakaoAccessToken, LocalDateTime.now(), refreshTokenResponse.token, refreshTokenResponse.expiryAt);
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

    private String extractUserNameFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount.containsKey("profile")) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                return profile.getOrDefault("nickname", "").toString();
            }
        }
        return "";
    }

    private String extractProfileImageFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount.containsKey("profile")) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                return profile.getOrDefault("profile_image_url", "").toString();
            }
        }
        return "";
    }
}
