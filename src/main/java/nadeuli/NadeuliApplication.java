package nadeuli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "nadeuli")  // ✅ 명시적으로 패키지 스캔 지정
public class NadeuliApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadeuliApplication.class, args);
    }
}
