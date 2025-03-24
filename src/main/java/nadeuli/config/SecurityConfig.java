/*
 * SecurityConfig.java
 * Spring Security 파일
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.19    경로 변경에 따른 import 변경
 * 박한철     2025.03.24    actuator 관련 처리 추가
 * 이홍비     2025.03.24    .requestMatchers("/actuator/**").permitAll()로 변경
 *                         .requestMatchers("/actuator/**").hasAnyAuthority("ACTUATOR_ADMIN") 으로 변경
 * ========================================================
 */


package nadeuli.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.auth.handler.CustomAuthenticationEntryPoint;
import nadeuli.auth.handler.CustomAuthorizationRequestResolver;
import nadeuli.auth.handler.CustomOAuth2SuccessHandler;
import nadeuli.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler; // ✅ 주입
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthorizationRequestResolver customResolver) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 설정 추가
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll()
                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
                        .requestMatchers("/join/**").permitAll()
                        .requestMatchers("/login", "/mypage").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/share/join").permitAll()
                        .requestMatchers("/api/place/register").permitAll()
                        .requestMatchers("/oauth2/authorization/kakao", "/oauth2/authorization/google").permitAll()
                        // ✅ 정적 리소스 허용 (CSS, JS, 이미지, 폰트)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/fonts/**", "favicon.ico").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
//                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth -> oauth
                        .loginPage("/login") // 커스텀 로그인 페이지 경로
                        .authorizationEndpoint(config -> config
                                .authorizationRequestResolver(customResolver)
                        )
                        .successHandler(customOAuth2SuccessHandler) // 로그인 성공 핸들러
//                        .defaultSuccessUrl("/mypage", true) // (선택) 기본 리다이렉트 경로 좀되라 제바ㅣㄹ
//                        .failureUrl("/login?error=true") // 실패 시 리다이렉트
                )
                .exceptionHandling(
                        config -> config.authenticationEntryPoint(customAuthenticationEntryPoint))
//                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // ✅ 프론트엔드 URL 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // ✅ 쿠키 전송 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
