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

        // ✅ OAuth Provider 정보 가져오기
        final String provider = userRequest.getClientRegistration().getRegistrationId();
        final String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info("🔹 [CustomOAuth2UserService] OAuth2 로그인 요청 - Provider: {}, userNameAttribute: {}", provider, userNameAttributeName);

        // ✅ 사용자 정보 처리 및 SecurityContext 반영
        OAuth2User authenticatedUser = processOAuthUser(oAuth2User, provider, userNameAttributeName);

        // ✅ SecurityContextHolder에 저장 (JWT 없이도 SecurityContext에서 접근 가능)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities())
        );

        log.info("✅ [CustomOAuth2UserService] SecurityContext 업데이트 완료 - 사용자: {}", authenticatedUser.getName());
        return authenticatedUser;
    }

    /**
     * ✅ OAuth 사용자 정보를 가져와서 SecurityContext에 저장하는 메서드
     */
    private OAuth2User processOAuthUser(OAuth2User oAuth2User, String provider, String userNameAttributeName) {
        final Map<String, Object> attributes = oAuth2User.getAttributes();

        // ✅ 기본 사용자 정보 추출
        final String email = getSafeAttribute(attributes, "email");
        final String name = getSafeAttribute(attributes, "name");
        final String profileImage = getSafeAttribute(attributes, "picture");

        log.info("🔹 [processOAuthUser] OAuth 사용자 정보 - Email: {}, Name: {}, Provider: {}", email, name, provider);

        if (email.isEmpty()) {
            log.warn("🚨 [processOAuthUser] 이메일 정보가 없음 - OAuth 로그인 실패");
            throw new OAuth2AuthenticationException("OAuth 로그인 실패: 이메일 정보를 찾을 수 없습니다.");
        }

        // ✅ 기존 사용자 조회 및 업데이트
        final User userEntity = userRepository.findByUserEmail(email)
                .orElseGet(() -> createNewUser(email, name, profileImage, provider));

        // ✅ `CustomOAuth2User`를 반환하여 SecurityContext에 적용
        return new CustomOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(userEntity.getUserRole().name())), // 권한 적용
                attributes,
                userNameAttributeName,
                userEntity
        );
    }

    /**
     * ✅ 신규 사용자 생성
     */
    private User createNewUser(final String email, final String name, final String profileImage, final String provider) {
        log.info("✅ [processOAuthUser] 신규 사용자 등록 - Email: {}", email);
        return userRepository.save(User.createNewUser(
                email, name, profileImage, provider, null,
                LocalDateTime.now(), // 🔹 마지막 로그인 시간 추가
                LocalDateTime.now().plusMonths(6) // 🔹 Refresh Token 만료일 추가 (Google 기준)
        ));
    }

    /**
     * ✅ 안전한 Attribute 값 가져오기 (예외 방지)
     */
    private String getSafeAttribute(final Map<String, Object> attributes, final String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : "";
    }
}
