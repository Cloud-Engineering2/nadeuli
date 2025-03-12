/* ItineraryCollaboratorService.java
 * ItineraryCollaborator 서비스
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
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import nadeuli.repository.ExpenseBookRepository;
import nadeuli.repository.ItineraryCollaboratorRepository;
import nadeuli.repository.ItineraryRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryCollaboratorService {
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final ItineraryRepository itineraryRepository;
    private final ExpenseBookRepository expenseBookRepository;

    public String getOwner(Itinerary itinerary) {

        // ItineraryCollaborator 조회
        ItineraryCollaborator itineraryCollaborator = itineraryCollaboratorRepository.findFirstByItinerary(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryCollaborator가 존재하지 않습니다"));

        // owner username
        return itineraryCollaborator.getUser().getUserName();
    }

}
