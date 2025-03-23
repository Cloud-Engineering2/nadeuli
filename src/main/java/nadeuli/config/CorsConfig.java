package nadeuli.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
////
////                // 본인: 마이페이지 등 인증이 필요한 auth API
////                registry.addMapping("/auth/**")
////                        .allowedOrigins("http://localhost:3000")
////                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
////                        .allowCredentials(true); // 쿠키 인증
////
////                // 팀원: REST API (예: itinerary, journal 등)
////                registry.addMapping("/api/**")
////                        .allowedOrigins("http://localhost:3000")
////                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
////                        .allowCredentials(true);
//            }
//        };
//    }
//}
