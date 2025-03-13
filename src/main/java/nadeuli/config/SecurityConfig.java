package nadeuli.config;

import lombok.extern.slf4j.Slf4j;
import nadeuli.service.KakaoOidcUserService;
import nadeuli.service.GoogleOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            KakaoOidcUserService kakaoOidcUserService,
            GoogleOidcUserService googleOidcUserService
    ) throws Exception {
        SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(kakaoOidcUserService)
                                .oidcUserService(googleOidcUserService)
                        )
                        .defaultSuccessUrl("/hello", true)
                )
                .sessionManagement(session -> session
                        .sessionFixation().none()
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                );

        return http.build();
    }
}
