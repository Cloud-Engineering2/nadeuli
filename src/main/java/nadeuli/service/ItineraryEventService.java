/* ItineraryEventService.java
 * ItineraryEvent 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정   2025.03.12    Itineraray id로 조회하는 메서드 추가
 *
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.ItineraryPerDay;
import nadeuli.repository.ItineraryEventRepository;
import nadeuli.repository.ItineraryPerDayRepository;
import nadeuli.repository.ItineraryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryEventService {
    private final ItineraryEventRepository itineraryEventRepository;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryRepository itineraryRepository;

    public List<ItineraryEvent> getAllByItineraryId(Long itineraryId) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        // ItineraryPerDay 조회
        List<ItineraryPerDay> itineraryPerDayList = itineraryPerDayRepository.findByItinerary(itinerary); // findAll
        // 모든 ItineraryEvent 조회
        List<ItineraryEvent> itineraryEventList = itineraryEventRepository.findByItineraryPerDayIn(itineraryPerDayList);

        return itineraryEventList;
    }


}
