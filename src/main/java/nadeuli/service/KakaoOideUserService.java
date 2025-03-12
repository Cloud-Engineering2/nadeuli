package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOideUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equals(provider)) {
            log.warn("🚨 [OAuth2UserService] Unsupported provider '{}' - Only 'kakao' is allowed.", provider);
            return oAuth2User;
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();

        return processOAuthUser(oAuth2User, provider, accessToken);
    }

    private OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String accessToken) {
        // OAuth2User로부터 기본 속성들을 가져옴 (기본값: 빈 문자열)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String profileImage = getSafeString(attributes, "picture");

        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = getSafeMap(attributes, "kakao_account");
            Map<String, Object> profile = getSafeMap(kakaoAccount, "profile");
            email = getSafeString(kakaoAccount, "email");
            name = getSafeString(profile, "nickname");
            profileImage = getSafeString(profile, "profile_image_url");
        }

        log.info("[processOAuthUser] Email: {}, Provider: {}, Name: {}, ProfileImage: {}, AccessToken: {}",
                email, provider, name, profileImage, accessToken);

        if (!email.isEmpty()) {
            final String finalEmail = email;
            final String finalName = name;
            final String finalProfileImage = profileImage;
            final String finalProvider = provider;
            final String finalAccessToken = accessToken;

            User userEntity = userRepository.findByUserEmail(finalEmail)
                    .map(existing -> updateExistingUser(existing, finalName, finalProfileImage, finalProvider, finalAccessToken))
                    .orElseGet(() -> createNewUser(finalEmail, finalName, finalProfileImage, finalProvider, finalAccessToken));

            userRepository.save(userEntity);
            log.info("[processOAuthUser] 사용자 저장 완료 - Email: {}", finalEmail);
        } else {
            log.warn("[processOAuthUser] 이메일 정보가 없음 - OAuth 로그인 실패");
            throw new OAuth2AuthenticationException("OAuth 로그인 실패: 이메일 정보를 찾을 수 없습니다.");
        }
        return oAuth2User;
    }


    private User updateExistingUser(User user, String name, String profileImage, String provider, String accessToken) {
        user.updateProfile(name, profileImage, provider, accessToken, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String provider, String accessToken) {
        return User.createNewUser(email, name, profileImage, provider, accessToken, LocalDateTime.now());
    }

    private String getSafeString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSafeMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof Map) ? (Map<String, Object>) value : Map.of();
    }
}
