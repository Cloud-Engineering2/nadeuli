package nadeuli.config;

import lombok.extern.slf4j.Slf4j;
import nadeuli.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 설정 추가
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll()
                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
                        .requestMatchers("/auth/user/**").permitAll()
                        .requestMatchers("/login", "/mypage").permitAll()
                        .requestMatchers("/oauth2/authorization/kakao", "/oauth2/authorization/google").permitAll()
                        // ✅ 정적 리소스 허용 (CSS, JS, 이미지, 폰트)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/mypage", true)
                        .failureUrl("/login?error=true").permitAll()
                )
                .logout(logout -> logout.logoutUrl("/logout").permitAll())
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
