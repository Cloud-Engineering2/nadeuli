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
 * 박한철    2025.02.26     페이징 방식의 내 일정리스트 조회로 변경
 * 박한철    2025.02.26     DB 구조 변경으로 인한 getItineraryTotal 수정 ,  일정 생성 파트 주석처리
 * 박한철    2025.02.26     일정 생성 파트 수정 완료
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.response.*;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final UserRepository userRepository;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryEventRepository itineraryEventRepository;

    // ===========================
    //  CREATE: 일정 생성
    // ===========================

    @Transactional
    public ItineraryCreateResponseDTO createItinerary(ItineraryCreateRequestDTO requestDTO, Long ownerId) {
        // 사용자 확인
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + ownerId));

        // 일정 생성
        ItineraryDTO itineraryDTO = requestDTO.getItinerary();
        Itinerary itineraryToSave = itineraryDTO.toEntity();
        Itinerary savedItinerary = itineraryRepository.save(itineraryToSave); // 새 변수로 저장

        // 하루 일정 생성 및 저장
        List<ItineraryPerDay> itineraryPerDayList = requestDTO.getItineraryPerDays().stream()
                .map(itineraryPerDayDTO -> itineraryPerDayDTO.toEntity(savedItinerary))
                .collect(Collectors.toList());

        itineraryPerDayRepository.saveAll(itineraryPerDayList);

        // 소유자 자동 등록
        ItineraryCollaborator collaborator = ItineraryCollaborator.of(owner, savedItinerary);
        itineraryCollaboratorRepository.save(collaborator);

        // ResponseDTO 변환 후 반환
        return ItineraryCreateResponseDTO.from(savedItinerary, itineraryPerDayList);
    }


    // ===========================
    //  READ: 내 일정 리스트 조회
    // ===========================

    public Page<ItineraryResponseDTO> getMyItineraries(Long userId, Pageable pageable) {
        // 1. 사용자(userId)가 참여한 일정(Itinerary) 조회
        Page<Object[]> results = itineraryRepository.findByUserIdWithRole(userId, pageable);

        // 2. 결과 변환
        return results.map(row -> {
            Itinerary itinerary = (Itinerary) row[0];  // Itinerary 객체
            String role = (String) row[1];  // Collaborator 역할 정보

            return ItineraryResponseDTO.from(itinerary, role);
        });
    }

// ===========================
//  READ: 특정 일정 조회 - Events 포함
// ===========================

    public ItineraryTotalResponseDTO getItineraryTotal(Long itineraryId, Long userId) {
        // 1. 일정 조회 (없으면 예외 발생)
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + itineraryId));

        // 2. 해당 일정에 속한 현재 사용자의 Collaborator 정보 조회
        ItineraryCollaborator collaborator = itineraryCollaboratorRepository
                .findByItinerary_IdAndUser_Id(itineraryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("이 일정에 대한 권한이 없습니다."));

        // 3. 해당 일정의 perDay 목록 조회
        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);

        // 4. 해당 일정의 perDay 목록을 이용하여 각 perDay의 이벤트 목록 조회
        List<ItineraryEvent> itineraryEvents = itineraryEventRepository.findByItineraryPerDayIn(itineraryPerDays);

        // 5. DTO 변환 후 반환
        return new ItineraryTotalResponseDTO(
                ItineraryResponseDTO.from(collaborator),  // ✅ Collaborator를 기반으로 DTO 변환
                itineraryPerDays.stream()
                        .map(ItineraryPerDaySimpleDTO::from)
                        .toList(), // ✅ ItineraryPerDay -> ItineraryPerDaySimpleDTO 변환
                itineraryEvents.stream()
                        .map(ItineraryEventSimpleDTO::from)
                        .toList()  // ✅ ItineraryEvent -> ItineraryEventSimpleDTO 변환
        );
    }


}
