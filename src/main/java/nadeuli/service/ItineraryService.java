/* ItineraryService.java
 * Itinerary 서비스
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

import nadeuli.dto.ItineraryCollaboratorDTO;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryEventDTO;
import nadeuli.dto.response.ItineraryEventSimpleDTO;
import nadeuli.dto.response.ItineraryResponseDTO;
import nadeuli.dto.response.ItineraryTotalResponseDTO;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.User;
import nadeuli.repository.ItineraryCollaboratorRepository;
import nadeuli.repository.ItineraryEventRepository;
import nadeuli.repository.ItineraryRepository;
import nadeuli.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final UserRepository userRepository;
    private final ItineraryEventRepository itineraryEventRepository; // ✅ 이벤트 리포지토리 추가

    // ===========================
    //  CREATE: 일정 생성
    // ===========================

    public ItineraryDTO createItinerary(String itineraryName, LocalDateTime startDate, LocalDateTime endDate, Long ownerId) {
        // 사용자 확인
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + ownerId));

        // 일정 생성
        Itinerary itinerary = Itinerary.of(itineraryName, startDate, endDate);
        itinerary = itineraryRepository.save(itinerary);

        // 소유자 자동 등록
        ItineraryCollaborator collaborator = ItineraryCollaborator.of(owner, itinerary);
        itineraryCollaboratorRepository.save(collaborator);

        return ItineraryDTO.from(itinerary);
    }

    // ===========================
    //  READ: 내 일정 리스트 조회
    // ===========================

    public List<ItineraryResponseDTO> getMyItineraries(Long userId) {
        List<Itinerary> itineraries = itineraryCollaboratorRepository.findByUser_Id(userId)
                .stream()
                .map(ItineraryCollaborator::getItinerary)
                .toList();  // ✅ 변경됨

        return itineraries.stream()
                .map(ItineraryResponseDTO::from)
                .toList();  // ✅ 변경됨
    }


    // ===========================
    //  READ: 특정 일정 조회 - Events 포함
    // ===========================

    public ItineraryTotalResponseDTO getItineraryTotal(Long itineraryId) {
        // 1. 일정 조회 (없으면 예외 발생)
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + itineraryId));

        // 2. 해당 일정의 이벤트 목록 조회
        List<ItineraryEvent> itineraryEvents = itineraryEventRepository.findByItinerary(itinerary);

        // 3. DTO 변환 후 반환
        return new ItineraryTotalResponseDTO(
                ItineraryResponseDTO.from(itinerary),
                itineraryEvents.stream()
                        .map(ItineraryEventSimpleDTO::from)  // ✅ 이벤트 DTO 변환
                        .toList()
        );
    }

}
