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
 *
 *
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.repository.ItineraryEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryEventService {
    private final ItineraryEventRepository itineraryEventRepository;
}
