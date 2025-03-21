/* ItineraryEventRestController.java
 * ItineraryEvent Rest 컨트롤러
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-03-21
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 고민정    2025.03.21     ItineraryEvent 조회 메서드

 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryEventDTO;
import nadeuli.service.ItineraryEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryEventRestController {

    private final ItineraryEventService itineraryEventService;

    // itinerary event id로 조회
    @GetMapping("/{iid}/events/{ieid}")
    public ResponseEntity<ItineraryEventDTO> getItineraryEvent (@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {

        Long itineraryId = Long.valueOf(iid);
        Long itineraryEventId = Long.valueOf(ieid);

        ItineraryEventDTO itineraryEventDto = itineraryEventService.retrieveItineraryEvent(itineraryEventId);
        return ResponseEntity.ok(itineraryEventDto);

    }
}
