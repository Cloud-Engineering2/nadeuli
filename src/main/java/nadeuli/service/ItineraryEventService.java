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
 * 이홍비   2025.03.22    Itinerary 에 포함된 ItineraryEvent 인지 확인
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryEventDTO;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.ItineraryPerDay;
import nadeuli.repository.ItineraryEventRepository;
import nadeuli.repository.ItineraryPerDayRepository;
import nadeuli.repository.ItineraryRepository;
import org.springframework.security.access.AccessDeniedException;
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


    public ItineraryEventDTO retrieveItineraryEvent(Long itineraryEventId) {
        ItineraryEvent itineraryEvent = itineraryEventRepository.findById(itineraryEventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryEvent가 존재하지 않습니다"));
        return ItineraryEventDTO.from(itineraryEvent);


    }


    
    // 해당 일정에 포함된 방문지인지 아닌지 확인
    public void checkItineraryEventIdInItinerary(Long itineraryId, Long itineraryEventId) {
        System.out.println("🔥 ItineraryEventService - checkItineraryEventIdInItinerary()");

        List<Long> ieidList = itineraryEventRepository.findItineraryEventIdsByItineraryId(itineraryId);

        if (!ieidList.contains(itineraryEventId)) {
            // ieidList 에 해당 id  포함 x => 해당 일정에 대한 방문지 아님
            System.out.println("❌ 해당 일정에 대한 방문지가 아닙니다 ❌");

            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
    }

}
