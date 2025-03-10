package nadeuli.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        //RedisTemplate Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        //RestTemplate Mock 설정 (Google Places API 응답 데이터)
        String mockResponse = """
                {
                    "places": [{
                        "id": "ChIJA3CU42aifDURaq-3csGXvuc",
                        "displayName": {"text": "서울역"},
                        "formattedAddress": "서울특별시 중구 한강대로 405",
                        "location": {
                            "latitude": 37.555946,
                            "longitude": 126.972317
                        },
                        "rating": 4.5,
                        "userRatingCount": 2000
                    }]
                }
                """;

        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));
    }

    @Test
    void testSearchPlaces_CachingWorks() {
        String query = "서울역";
        double lat = 37.555946;
        double lng = 126.972317;
        double radius = 1000;
        String cacheKey = String.format("search_places:%s:%.6f:%.6f:%.1f", query, lat, lng, radius);

        String response = placeService.searchPlaces(query, lat, lng, radius);
        assertNotNull(response, "API 응답이 null 입니다.");
        assertTrue(response.contains("\"places\""), "API 응답에 'places' 키가 포함되지 않음.");

        verify(valueOperations, times(1)).set(eq(cacheKey), anyString(), any());

        when(valueOperations.get(cacheKey)).thenReturn(response);
        String cachedResponse = placeService.searchPlaces(query, lat, lng, radius);
        assertNotNull(cachedResponse, "캐시에서 검색 결과를 찾을 수 없음.");
        assertTrue(cachedResponse.contains("\"places\""), "캐시 응답에 'places' 키가 포함되지 않음.");

        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(String.class));
    }
}
