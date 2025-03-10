package nadeuli.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 🔹 Provider 값 가져오기
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        log.info("🔹 OAuth2 로그인 요청 - Provider: {}, userNameAttribute: {}", provider, userNameAttributeName);

        return processOAuthUser(oAuth2User, provider, userNameAttributeName);
    }

    /**
     * ✅ OAuth 사용자 정보를 가져와서 DB에 저장하는 메서드
     */
    private OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String userNameAttributeName) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // ✅ 기본 정보 가져오기 (Google, Kakao 등)
        String email = getSafeAttribute(attributes, "email");
        String name = getSafeAttribute(attributes, "name");
        String profileImage = getSafeAttribute(attributes, "picture");
        String refreshToken = "";  // 기본값 설정

        // ✅ Google 로그인 시 `refresh_token` 저장 (최초 로그인 시에만 제공됨)
        if ("google".equals(provider)) {
            refreshToken = getSafeAttribute(attributes, "refresh_token");
        }

        // ✅ 카카오 로그인 시 데이터 매핑
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = getSafeMap(attributes, "kakao_account");
            Map<String, Object> profile = getSafeMap(kakaoAccount, "profile");

            email = getSafeAttribute(kakaoAccount, "email");
            name = getSafeAttribute(profile, "nickname");
            profileImage = getSafeAttribute(profile, "profile_image_url");
        }

        log.info("🔹 [processOAuthUser] OAuth 로그인 요청 - Email: {}, Provider: {}, Name: {}, ProfileImage: {}, RefreshToken: {}",
                email, provider, name, profileImage, refreshToken);

        // ✅ 기존 사용자 조회 및 업데이트
        Optional<User> existingUserOpt = userRepository.findByUserEmail(email);
        User userEntity;

        if (existingUserOpt.isPresent()) {
            userEntity = existingUserOpt.get();
            boolean isUpdated = false;

            if (!userEntity.getUserName().equals(name) || !userEntity.getProfileImage().equals(profileImage) || !userEntity.getProvider().equals(provider)) {
                userEntity.updateProfile(name, profileImage, provider, refreshToken,
                        LocalDateTime.now(), // 🔹 마지막 로그인 시간 추가
                        LocalDateTime.now().plusDays(14) // 🔹 Refresh Token 만료일 추가
                );
                isUpdated = true;
            }

            if (isUpdated) {
                userRepository.save(userEntity);
                log.info("✅ [processOAuthUser] 기존 사용자 정보 업데이트 완료 - Email: {}", email);
            }
        } else {
            userEntity = userRepository.save(User.createNewUser(
                    email, name, profileImage, provider, refreshToken,
                    LocalDateTime.now(), // 🔹 마지막 로그인 시간 추가
                    LocalDateTime.now().plusDays(14) // 🔹 Refresh Token 만료일 추가
            ));
            log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", email);
        }

        // ✅ SecurityContextHolder에 인증 정보 강제 저장
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName,
                userEntity
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("✅ SecurityContext에 사용자 인증 정보 저장 완료 - Email: {}", email);

        return customOAuth2User;
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (예외 방지)
     */
    private String getSafeAttribute(Map<String, Object> attributes, String key) {
        try {
            Object value = attributes.get(key);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("🚨 [getSafeAttribute] {} 속성을 가져오는 중 오류 발생: {}", key, e.getMessage());
            return "";
        }
    }

    /**
     * ✅ 안전한 Map 변환 메서드
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSafeMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap(); // ⚠️ null 대신 빈 Map 반환 (NPE 방지)
    }
}
