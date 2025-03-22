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
 * 이홍비    2025.03.22   AccessDeniedException 오류 메시지 "대한" 추가
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import nadeuli.entity.constant.CollaboratorRole;
import nadeuli.repository.ExpenseBookRepository;
import nadeuli.repository.ItineraryCollaboratorRepository;
import nadeuli.repository.ItineraryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryCollaboratorService {

    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;

    public CollaboratorRole getUserRole(Long userId, Long itineraryId) {
        return itineraryCollaboratorRepository.findByUserIdAndItineraryId(userId, itineraryId)
                .map(collaborator -> CollaboratorRole.from(collaborator.getIcRole()))
                .orElseThrow(() -> new AccessDeniedException("해당 일정에 대한 접근 권한이 없습니다."));
    }

    //일정파트는 ROLE_OWNER만 가능하므로 일정파트인지 Expense파트인지에 따라 추가 검사가 이루어짐
    public void checkEditPermission(Long userId, Long itineraryId, boolean isExpensePart) {
        CollaboratorRole role = getUserRole(userId, itineraryId);

        // 일정 파트 수정은 OWNER만 가능
        if (!isExpensePart && role != CollaboratorRole.ROLE_OWNER) {
            throw new AccessDeniedException("일정 수정은 OWNER만 가능합니다.");
        }
    }

    //일정파트용 메서드 오버로딩
    public void checkViewPermission(Long userId, Long itineraryId) {
        getUserRole(userId, itineraryId); // OWNER/GUEST 둘 다 VIEW 가능
    }



    public String getOwner(Itinerary itinerary) {

        // ItineraryCollaborator 조회
        ItineraryCollaborator itineraryCollaborator = itineraryCollaboratorRepository.findFirstByItinerary(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryCollaborator가 존재하지 않습니다"));

        // owner username
        return itineraryCollaborator.getUser().getUserName();
    }

}
