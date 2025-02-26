/* OAuthSecurityConfig.java
 * 구글 및 카카오 OAuth 2.0 연동 위한 시큐리티 설정
 * 해당 파일 설명
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 김대환      2.24        카카오 길찾기 URL 반환 매핑 경로 권한
 * 국경민      2.25        구글 OAuth 2.0 로그인 및 카카오 OAuth 연동 통합
 * ========================================================
 */

package nadeuli.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class OAuthSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("api/admin/unlink/**").permitAll()
                        .requestMatchers("/travel/**").permitAll()
                        .requestMatchers("/oauth-direction").permitAll() // 기존 카카오 URL을 통합하여 수정
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/loginSuccess", true)
                        .failureUrl("/loginFailure")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(combinedOAuth2UserService())
                        )
                );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> combinedOAuth2UserService() {
        return userRequest -> {
            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
            String provider = userRequest.getClientRegistration().getRegistrationId(); // 제공자 정보 추가
            String accessToken = userRequest.getAccessToken().getTokenValue();

            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest().getSession();
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("provider", provider); // 세션에 제공자 정보 추가

            return oAuth2User;
        };
    }
}
