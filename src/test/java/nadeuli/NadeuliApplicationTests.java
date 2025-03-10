package nadeuli;

import nadeuli.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class NadeuliApplicationTests {

    @MockBean
    private EmailNotificationService emailNotificationService; // ✅ 메일 서비스 Mock 처리

    @Test
    void contextLoads() {
        // ✅ 간단한 테스트 실행
    }
}
