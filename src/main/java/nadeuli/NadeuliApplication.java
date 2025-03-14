package nadeuli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication(scanBasePackages = "nadeuli")
public class NadeuliApplication {

    public static void main(String[] args) {
        SpringApplication.run(NadeuliApplication.class, args);
    }

}
