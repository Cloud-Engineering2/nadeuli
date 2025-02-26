/* GooglePlaceService
 * GooglePlaceService 파일 - api 키 호출
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 *
 *
 */

package nadeuli.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GooglePlacesService {

    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getPlaceData(String query) {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input=%s" +
                        "&inputtype=textquery&fields=place_id,name&key=%s",
                query, apiKey
        );

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }
}
