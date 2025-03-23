/*
 * GoogleOidcUserService.java
 * Google OIDC 로그인 인증 처리 및 사용자 정보 관리 서비스
 * - 사용자 정보를 기반으로 회원 등록 또는 업데이트 처리
 * - JWT Access/Refresh Token 생성 및 사용자 정보에 저장
 * 작성자 : 김대환, 국경민
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 김대환, 국경민    2025.03.19      최초 작성 - Google OIDC 인증 및 JWT 토큰 발급 로직 구현
 * 김대환    2025.03.19     User_Role 기본값 지정
 * 박한철    2025.03.23     jwt with redis로 바꾸면서 리펙토링
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import nadeuli.dto.response.TokenResponse;
import nadeuli.auth.oauth.CustomOidcUser;
import nadeuli.common.util.JwtUtils;
import nadeuli.entity.User;

import nadeuli.common.enums.UserRole;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final JwtRedisService jwtRedisService;
    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toLowerCase(Locale.ROOT);

        if (!"google".equals(provider)) {
            log.warn("[GoogleOidcUserService] Unsupported provider '{}'", provider);
            return oidcUser;
        }

        String googleAccessToken = userRequest.getAccessToken().getTokenValue();
        log.debug("[Google OAuth] Google Access Token: {}", googleAccessToken);

        return processOidcUser(oidcUser, provider, googleAccessToken);
    }

    private OidcUser processOidcUser(OidcUser oidcUser, String provider, String googleAccessToken) {
        Map<String, Object> attributes = oidcUser.getAttributes();
        String email = getSafeString(attributes, "email");
        String name = getSafeString(attributes, "name");
        String picture = getSafeString(attributes, "picture");

        log.debug("[Google OIDC] Email: {}, Name: {}, Picture: {}", email, name, picture);

        User userEntity = userRepository.findByUserEmailAndProvider(email, provider)
                .map(existing -> updateExistingUser(existing, name, picture, googleAccessToken))
                .orElseGet(() -> {
                    User newUser = createNewUser(email, name, picture, googleAccessToken);
                    newUser.setUserRole(UserRole.MEMBER);
                    return newUser;
                });

        userRepository.save(userEntity);


        // RefreshToken + Redis 저장
        String sessionId = UUID.randomUUID().toString();
        TokenResponse refreshTokenResponse = JwtUtils.generateRefreshToken(email);
        jwtRedisService.storeRefreshToken(email, sessionId, refreshTokenResponse);
        log.info("[Redis exported Google Refresh Token] sessionId: {}, email: {}", sessionId, email);

        // Updated Attributes
        Map<String, Object> updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("refreshToken", refreshTokenResponse.token);
        updatedAttributes.put("sessionId", sessionId);
        updatedAttributes.put("email", email); // 혹시 모를 누락 방지용



        return new CustomOidcUser(
                oidcUser.getAuthorities(),
                updatedAttributes,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }


    @Value("${cloudfront.url}")
    private String cloudFrontUrl;

    private User updateExistingUser(User user, String name, String profileImage, String googleAccessToken) {
        boolean tokenUpdated = !googleAccessToken.equals(user.getProviderRefreshToken());

        String currentProfileImage = user.getProfileImage();

        boolean isUserProfileFromS3 = (currentProfileImage != null && currentProfileImage.startsWith(cloudFrontUrl));

        String updatedName = user.getUserName();

        String updatedProfileImage = isUserProfileFromS3 ? currentProfileImage : profileImage;

        if (tokenUpdated) {
            log.info("Google Access Token 변경 감지! 기존 값: {}, 새 값: {}", user.getProviderRefreshToken(), googleAccessToken);
            user.updateProfile(updatedName, updatedProfileImage, "google", googleAccessToken, LocalDateTime.now());

        } else {
            log.info("기존 Google Access Token 유지: {}", googleAccessToken);
            user.updateProfile(updatedName, updatedProfileImage, "google", null, LocalDateTime.now());
        }

        return user;
    }

    private User createNewUser(String email, String name, String profileImage, String googleAccessToken) {
        TokenResponse refreshTokenResponse = JwtUtils.generateRefreshToken(email);
        return User.createNewUser(email, name, profileImage, "google", googleAccessToken, LocalDateTime.now());
    }

    private String getSafeString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}
