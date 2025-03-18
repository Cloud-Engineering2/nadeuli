package nadeuli.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler; // ✅ 주입

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/login", "/hello").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth
                        .successHandler(customOAuth2SuccessHandler) // ✅ 여기 이렇게 넣으시면 됩니다
                        .failureUrl("/login?error=true")
                )
//                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http,
//                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll()
//                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
//                        .requestMatchers("/auth/user/**").permitAll()
//                        .requestMatchers("login","hello").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(oauth -> oauth
//                        .successHandler(customOAuth2SuccessHandler)
////                      .defaultSuccessUrl("/auth/success", true)
//                        .failureUrl("/login?error=true").permitAll()
//                )
//                .logout(logout -> logout.logoutUrl("/logout").permitAll())
//                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
//                .csrf(csrf -> csrf.disable());
//
//        return http.build();
//    }

}

