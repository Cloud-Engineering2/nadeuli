package nadeuli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // ✅ JPA Auditing 활성화 (별도 JpaConfig 필요 없음)
public class NadeuliApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadeuliApplication.class, args);
    }
}
