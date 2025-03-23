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
 * 국경민, 김대환    2025.03.19     최초 작성 - Kakao OAuth 로그인 처리 및 JWT 토큰 발급 로직 구현
 * 김대환    2025.03.19     User_Role 기본값 지정
 * 박한철    2025.03.20     Email과 Provider를 동시에 검증하도록 수정
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.common.util.JwtUtils;
import nadeuli.dto.response.TokenResponse;
import nadeuli.entity.User;
import nadeuli.common.enums.UserRole;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOidcUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtRedisService jwtRedisService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toLowerCase(Locale.ROOT);

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
        String providerId = Optional.ofNullable(attributes.get("id"))
                            .map(String::valueOf)
                            .orElse(null);
        String email = getNestedSafeString(attributes, "kakao_account", "email");
        String name = getNestedSafeString(attributes, "kakao_account", "profile", "nickname");
        String profileImage = getNestedSafeString(attributes, "kakao_account", "profile", "profile_image_url");
        log.info("[{} OAuth] Email: {}, Name: {}, ProfileImage: {}", provider, email, name, profileImage);

        User userEntity = userRepository.findByProviderIdAndProvider(providerId, provider)
                .map(existing -> updateExistingUser(existing, name, profileImage, kakaoAccessToken))
                .orElseGet(() -> {
                    User newUser = createNewUser(email, name, profileImage, providerId, kakaoAccessToken);
                    newUser.setUserRole(UserRole.MEMBER);
                    return newUser;
                });

        userRepository.save(userEntity);

        // RefreshToken + Redis 저장
        String sessionId = UUID.randomUUID().toString();
        TokenResponse refreshTokenResponse = JwtUtils.generateRefreshToken(email);
        jwtRedisService.storeRefreshToken(email, sessionId, refreshTokenResponse);
        log.info("[Redis exported Kakao Refresh Token] sessionId: {}, email: {}", sessionId, email);


        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("refreshToken", refreshTokenResponse.token);
        updatedAttributes.put("sessionId", sessionId);
        updatedAttributes.put("email", email);


        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                updatedAttributes,
                "email"
        );
    }

    @Value("${cloudfront.url}")
    private String cloudFrontUrl;

    private User updateExistingUser(User user, String name, String profileImage, String kakaoAccessToken) {
        log.info("기존 사용자 정보 업데이트 - Email: {}, AccessToken 변경 여부: {}",
                user.getUserEmail(),
                !kakaoAccessToken.equals(user.getProviderAccessToken()));

        String currentProfileImage = user.getProfileImage();

        boolean isUserProfileFromS3 = (currentProfileImage != null && currentProfileImage.startsWith(cloudFrontUrl));

        String updatedName = user.getUserName();

        String updatedProfileImage = isUserProfileFromS3 ? currentProfileImage : profileImage;

        user.updateProfile(updatedName, updatedProfileImage, "kakao", kakaoAccessToken, LocalDateTime.now());
        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String providerId, String kakaoAccessToken) {
        TokenResponse refreshTokenResponse = JwtUtils.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "kakao", providerId, kakaoAccessToken, LocalDateTime.now());
    }

    private String getNestedSafeString(Map<String, Object> parent, String... keys) {
        Object current = parent;
        for (String key : keys) {
            if (!(current instanceof Map)) return "";
            current = ((Map<?, ?>) current).get(key);
            if (current == null) return "";
        }
        return current.toString();
    }

//    private String extractEmailFromAttributes(Map<String, Object> attributes) {
//        if (attributes.containsKey("email")) {
//            return attributes.get("email").toString();
//        }
//        if (attributes.containsKey("kakao_account")) {
//            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
//            if (kakaoAccount.containsKey("email")) {
//                return kakaoAccount.get("email").toString();
//            }
//        }
//        return null;
//    }
//
//    private String extractUserNameFromAttributes(Map<String, Object> attributes) {
//        if (attributes.containsKey("kakao_account")) {
//            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
//            if (kakaoAccount.containsKey("profile")) {
//                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
//                return profile.getOrDefault("nickname", "").toString();
//            }
//        }
//        return "";
//    }
//
//    private String extractProfileImageFromAttributes(Map<String, Object> attributes) {
//        if (attributes.containsKey("kakao_account")) {
//            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
//            if (kakaoAccount.containsKey("profile")) {
//                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
//                return profile.getOrDefault("profile_image_url", "").toString();
//            }
//        }
//        return "";
//    }
}
