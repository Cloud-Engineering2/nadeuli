package nadeuli.config;

import lombok.extern.slf4j.Slf4j;
import nadeuli.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/auth/unlink/**").permitAll()
                        .requestMatchers("/auth/refresh", "/auth/refresh/**").permitAll()
                        .requestMatchers("/auth/user/**").permitAll()
                        .requestMatchers("login","hello").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/hello", true)
                        .failureUrl("/login?error=true").permitAll()
                )
                .logout(logout -> logout.logoutUrl("/logout").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

}

