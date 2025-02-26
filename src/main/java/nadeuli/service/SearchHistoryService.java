/* SearchHistoryService
 * SearchHistoryService 파일 - DB 저장 전략
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final StringRedisTemplate redisTemplate;
    private final PlaceRepository placeRepository;

    private static final String SEARCH_HISTORY_KEY = "search:history";
    private static final String PLACE_NAME_HASH_KEY = "search:places";
    private static final int MAX_HISTORY_SIZE = 30;

    @Transactional
    public void saveSearchHistory(String pid, String placeName) {
        try {
            log.info("🔹 saveSearchHistory() 호출됨: place_id={}, place_name={}", pid, placeName);

            placeRepository.findByGooglePlaceId(pid)
                    .ifPresentOrElse(place -> {
                        place.incrementSearchCount();
                        log.info("기존 장소 검색 횟수 증가: {}", place.getSearchCount());
                        placeRepository.saveAndFlush(place);
                    }, () -> {
                        Place newPlace = new Place(pid, placeName);
                        log.info("새로운 장소 저장: {}", newPlace);
                        placeRepository.saveAndFlush(newPlace);
                    });

            log.info("데이터 저장 완료!");

            log.info("place_id 저장: {}", pid);
            redisTemplate.opsForList().remove(SEARCH_HISTORY_KEY, 0, pid);
            redisTemplate.opsForList().leftPush(SEARCH_HISTORY_KEY, pid);
            redisTemplate.opsForList().trim(SEARCH_HISTORY_KEY, 0, MAX_HISTORY_SIZE - 1);

            log.info("place_name 저장: {} -> {}", pid, placeName);
            redisTemplate.opsForHash().put(PLACE_NAME_HASH_KEY, pid, placeName);

            redisTemplate.expire(SEARCH_HISTORY_KEY, 3, TimeUnit.DAYS);
            redisTemplate.expire(PLACE_NAME_HASH_KEY, 3, TimeUnit.DAYS);

            log.info("Redis 캐싱 완료");

        } catch (Exception e) {
            log.error("데이터 저장 중 오류 발생", e);
        }
    }

    public List<String> getRecentSearchHistory() {
        return redisTemplate.opsForList().range(SEARCH_HISTORY_KEY, 0, -1);
    }

    public String getPlaceNameByPid(String pid) {
        return (String) redisTemplate.opsForHash().get(PLACE_NAME_HASH_KEY, pid);
    }
}