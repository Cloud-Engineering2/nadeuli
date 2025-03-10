package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlaceControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRegisterPlaceAPI() {
        PlaceRequest request = new PlaceRequest(
                "user123",
                "ChIJA3CU42aifDURaq-3csGXvuc",
                "서울역",
                "서울특별시 중구 한강대로 405",
                "서울특별시 중구",
                37.555946,
                126.972317
        );

        HttpEntity<PlaceRequest> entity = new HttpEntity<>(request);

        // When (API 호출)
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/place/register",
                HttpMethod.POST,
                entity,
                String.class
        );

        // 디버깅 로그 출력
        System.out.println("응답 상태 코드: " + response.getStatusCode());
        System.out.println("응답 본문: " + response.getBody());

        // 기존 장소가 존재하면 200, 새로 추가되면 201을 허용하도록 변경
        assertTrue(response.getStatusCode().value() == 200 || response.getStatusCode().value() == 201,
                "API 응답 상태 코드가 200 또는 201이 아님");

        assertNotNull(response.getBody(), "API 응답 본문이 null");
    }

    @Test
    void testSearchPlaceAPI() {
        PlaceRequest request = new PlaceRequest(
                "user123",
                "ChIJA3CU42aifDURaq-3csGXvuc",
                "서울역",
                "서울 중심 기차역",
                "서울특별시 중구 한강대로 405",
                37.555946,
                126.972317
        );

        HttpEntity<PlaceRequest> entity = new HttpEntity<>(request);

        // When (API 호출)
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/place/search",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // 디버깅 로그 출력
        System.out.println("응답 상태 코드: " + response.getStatusCode());

        // Then (결과 검증)
        assertEquals(200, response.getStatusCode().value(), "API 응답 상태 코드가 200이 아님");
    }


}
