/* AppConfig.java
 * RestTemplate Bean을 생성하여 주입
 * 해당 파일 설명
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.25       RestTemplate Bean을 생성하여 OAuthUnlinkService에서 주입받게하기
 * 국경민      3.03       파악을 위해 주석추가
 * ========================================================
 */
package nadeuli.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
