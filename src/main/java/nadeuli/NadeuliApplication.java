package nadeuli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ComponentScan(basePackages = "nadeuli")  // ✅ 명시적으로 패키지 스캔 지정
@EnableJpaAuditing  // ✅ JPA Auditing 활성화 (별도 JpaConfig 필요 없음)
public class NadeuliApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadeuliApplication.class, args);
    }
}
