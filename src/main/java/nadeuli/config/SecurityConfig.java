package nadeuli.config;

import lombok.extern.slf4j.Slf4j;
import nadeuli.service.KakaoOideUserService;
import nadeuli.service.GoogleOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            KakaoOideUserService kakaoOideUserService,
            GoogleOidcUserService customOidcUserService
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(kakaoOideUserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .defaultSuccessUrl("/", true)
                );


        return http.build();
    }
}
