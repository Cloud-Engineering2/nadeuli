package nadeuli.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOidcUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equals(provider)) {
            log.warn("[KakaoOidcUserService] Unsupported provider '{}' - Only 'kakao' is allowed.", provider);
            return oAuth2User;
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();

        return processOAuthUser(oAuth2User, provider, accessToken, request, response);
    }

    public OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String accessToken,
                                       HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmailFromAttributes(attributes);
        String name = getSafeString(attributes, "name");
        String profileImage = getSafeString(attributes, "picture");

        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = getSafeMap(attributes, "kakao_account");
            Map<String, Object> profile = getSafeMap(kakaoAccount, "profile");
            email = getSafeString(kakaoAccount, "email");
            name = getSafeString(profile, "nickname");
            profileImage = getSafeString(profile, "profile_image_url");
        }

        log.info("[Kakao OAuth] Email: {}, Provider: {}, Name: {}, ProfileImage: {}, AccessToken: {}",
                email, provider, name, profileImage, accessToken);

        if (email == null || email.isEmpty()) {
            log.warn("[Kakao OAuth] 이메일 정보가 없음 - OAuth 로그인 실패");
            throw new OAuth2AuthenticationException("OAuth 로그인 실패: 이메일 정보를 찾을 수 없습니다.");
        }

        final String finalEmail = email;
        final String finalName = name;
        final String finalProfileImage = profileImage;
        final String finalProvider = provider;
        final String finalAccessToken = accessToken;

        User userEntity = userRepository.findByUserEmail(finalEmail)
                .map(existing -> updateExistingUser(existing, finalName, finalProfileImage, finalProvider, finalAccessToken))
                .orElseGet(() -> createNewUser(finalEmail, finalName, finalProfileImage, finalProvider, finalAccessToken));

        userRepository.save(userEntity);
        log.info("[Kakao OAuth] 사용자 저장 완료 - Email: {}", finalEmail);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userEntity, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        log.info("SecurityContext 업데이트 완료 - 사용자: {}", finalEmail);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        log.info("SecurityContext가 세션에 저장됨 - Session ID: {}", session.getId());

        return oAuth2User;
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
