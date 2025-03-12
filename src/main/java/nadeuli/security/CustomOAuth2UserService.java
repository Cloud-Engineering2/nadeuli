/* CustomOAuth2UserService.java
 * OAuth 사용자 정보 로드 및 인증 처리
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       OAuth2 사용자 정보 로드 및 인증 처리 초안
 * 국경민      03-05       사용자 정보 업데이트 로직 추가
 * 국경민      03-05       Google, Kakao 데이터 매핑 로직 개선
 * 국경민      03-06       예외 처리 보강 및 로그 추가
 * 국경민      03-12       불필요한 변수 제거 및 성능 최적화
 * 국경민      03-12       NPE 방지 및 OAuth 인증 개선
 * 국경민      03-12       람다식 내부 변수 `final` 처리 및 import 정리 (컴파일 오류 해결)
 * ========================================================
 */

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


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // ✅ Provider 값 가져오기
        final String provider = userRequest.getClientRegistration().getRegistrationId();
        final String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info("🔹 [OAuth2 로그인 요청] Provider: {}, userNameAttribute: {}", provider, userNameAttributeName);

        return processOAuthUser(oAuth2User, provider, userNameAttributeName);
    }

    /**
     * ✅ OAuth 사용자 정보를 가져와서 DB에 저장하는 메서드
     */
    private OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String userNameAttributeName) {
        final Map<String, Object> attributes = oAuth2User.getAttributes();

        // ✅ 기본 사용자 정보 추출
        final String extractedEmail = getSafeAttribute(attributes, "email");
        final String extractedName = getSafeAttribute(attributes, "name");
        final String extractedProfileImage = getSafeAttribute(attributes, "picture");
        final String refreshToken;

        // ✅ Google 로그인 시 Refresh Token 저장 (최초 로그인에서만 제공됨)
        if ("google".equals(provider)) {
            refreshToken = getSafeAttribute(attributes, "refresh_token");
        } else {
            refreshToken = "";
        }

        // ✅ 카카오 로그인 시 데이터 매핑
        final String email;
        final String name;
        final String profileImage;
        if ("kakao".equals(provider)) {
            final Map<String, Object> kakaoAccount = getSafeMap(attributes, "kakao_account");
            final Map<String, Object> profile = getSafeMap(kakaoAccount, "profile");

            email = getSafeAttribute(kakaoAccount, "email");
            name = getSafeAttribute(profile, "nickname");
            profileImage = getSafeAttribute(profile, "profile_image_url");
        } else {
            email = extractedEmail;
            name = extractedName;
            profileImage = extractedProfileImage;
        }

        log.info("🔹 [processOAuthUser] Email: {}, Provider: {}, Name: {}, ProfileImage: {}, RefreshToken: {}",
                email, provider, name, profileImage, refreshToken);

        if (email.isEmpty()) {
            log.warn("🚨 [processOAuthUser] 이메일 정보가 없음 - OAuth 로그인 실패");
            throw new OAuth2AuthenticationException("OAuth 로그인 실패: 이메일 정보를 찾을 수 없습니다.");
        }

        // ✅ 기존 사용자 조회 및 업데이트
        final User userEntity = userRepository.findByUserEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, name, profileImage, provider, refreshToken))
                .orElseGet(() -> createNewUser(email, name, profileImage, provider, refreshToken));

        // ✅ SecurityContextHolder에 인증 정보 강제 저장
        final CustomOAuth2User customOAuth2User = new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName,
                userEntity
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities())
        );

        log.info("✅ SecurityContext에 사용자 인증 정보 저장 완료 - Email: {}", email);
        return customOAuth2User;
    }

    /**
     * ✅ 기존 사용자 정보 업데이트
     */
    private User updateExistingUser(final User existingUser, final String name, final String profileImage,
                                    final String provider, final String refreshToken) {
        boolean isUpdated = false;

        if (!existingUser.getUserName().equals(name) || !existingUser.getProfileImage().equals(profileImage) ||
                !existingUser.getProvider().equals(provider)) {
            existingUser.updateProfile(name, profileImage, provider, refreshToken,
                    LocalDateTime.now(),  // 🔹 마지막 로그인 시간 추가
                    LocalDateTime.now().plusDays(14)  // 🔹 Refresh Token 만료일 추가
            );
            isUpdated = true;
        }

        if (isUpdated) {
            userRepository.save(existingUser);
            log.info("✅ [processOAuthUser] 기존 사용자 정보 업데이트 완료 - Email: {}", existingUser.getUserEmail());
        }
        return existingUser;
    }

    /**
     * ✅ 신규 사용자 생성
     */
    private User createNewUser(final String email, final String name, final String profileImage,
                               final String provider, final String refreshToken) {
        log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", email);
        return userRepository.save(User.createNewUser(
                email, name, profileImage, provider, refreshToken,
                LocalDateTime.now(), // 🔹 마지막 로그인 시간 추가
                LocalDateTime.now().plusDays(14) // 🔹 Refresh Token 만료일 추가
        ));
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (예외 방지)
     */
    private String getSafeAttribute(final Map<String, Object> attributes, final String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * ✅ 안전한 Map 변환 메서드
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getSafeMap(final Map<String, Object> attributes, final String key) {
        Object value = attributes.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap(); // ⚠️ null 대신 빈 Map 반환 (NPE 방지)
    }
}
