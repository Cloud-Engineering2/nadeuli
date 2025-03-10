/* KakaoSecurityConfig.java
 * 카카오 및 기타 서비스 연동 위한 시큐리티
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 김대환    2025.02.24     카카오 길찾기 URL 반환 매핑 경로 권한
 * 박한철    2025.02.25     비로그인상에서 테스트하기 위한 권한 추가
 * 이홍비    2025.02.25     journal test - permitAll() 처리
 * 이홍비    2025.02.25     정적 자원 - fonts - permitAll() 처리
 * 이홍비    2025.03.06     정적 자원 (pic-icon) & error - permitAll() 처리
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
public class KakaoSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/","/api/**", "/itinerary/**", "/css/**", "/js/**", "/images/**", "/fonts/**", "favicon.ico", "/pic-icon/**").permitAll()
                        .requestMatchers("/api/itineraries/**", "/itineraries/**").permitAll() // journal test 용 추후 삭제
                        .requestMatchers("api/admin/unlink/**").permitAll()
                        .requestMatchers("/travel/**").permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .requestMatchers("/join/**").permitAll()
                        .requestMatchers("/kakao-direction").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/loginSuccess", true)
                        .failureUrl("/loginFailure")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService())
                        )
                );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return userRequest -> {
            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
            String accessToken = userRequest.getAccessToken().getTokenValue();

            HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest().getSession();
            session.setAttribute("accessToken", accessToken);

            return oAuth2User;
        };
    }
}
