package nadeuli.controller;

/* KakaoMapController.java
 * 길찾기 조회시 결과를 URL로 반환
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

import org.springframework.web.bind.annotation.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
public class KakaoMapController {

    private static final String KAKAO_MAP_URL = "https://map.kakao.com/?sName=";

    @GetMapping("/kakao-direction")
    public String getKakaoDirectionUrl(@RequestParam String departure, @RequestParam String destination) {
        try {
            String encodedQuery1 = URLEncoder.encode(departure, "UTF-8");
            String encodedQuery2 = URLEncoder.encode(destination, "UTF-8");
            return KAKAO_MAP_URL + encodedQuery1 + "&eName=" + encodedQuery2;
        } catch (UnsupportedEncodingException e) {
            return "Encoding error";
        }
    }
}
