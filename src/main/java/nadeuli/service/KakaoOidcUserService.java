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

        return processOAuthUser(oAuth2User, provider);
    }

    public OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmailFromAttributes(attributes);
        String name = extractUserNameFromAttributes(attributes);
        String profileImage = extractProfileImageFromAttributes(attributes);

        log.info("[{} OAuth] Email: {}, Name: {}, ProfileImage: {}", provider, email, name, profileImage);

        User userEntity = userRepository.findByUserEmail(email)
                .map(existing -> updateExistingUser(existing, name, profileImage))
                .orElseGet(() -> createNewUser(email, name, profileImage));

        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        userEntity.updateRefreshToken(refreshTokenResponse.token, refreshTokenResponse.expiryAt);
        userRepository.save(userEntity);

        String accessToken = jwtTokenService.generateAccessToken(email);

        log.info("[{} OAuth] JWT Access Token 발급 완료: {}", provider, accessToken);
        log.info("[{} OAuth] JWT Refresh Token 발급 완료: {}", provider, refreshTokenResponse.token);

        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("accessToken", accessToken);
        updatedAttributes.put("email", email);

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                updatedAttributes,
                "email"
        );
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


    private User updateExistingUser(User user, String name, String profileImage) {
        user.updateProfile(name, profileImage, "kakao", null, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage) {
        JwtTokenService.TokenResponse refreshTokenResponse = jwtTokenService.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "kakao", null, LocalDateTime.now(), refreshTokenResponse.token, refreshTokenResponse.expiryAt);
    }
}
