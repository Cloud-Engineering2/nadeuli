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

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.request.ItineraryTotalCreateRequestDTO;
import nadeuli.dto.response.*;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final UserRepository userRepository;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final PlaceRepository placeRepository;
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

// =====================================
//  CREATE & UPDATE: 특정 일정 저장 및 수정 ( Event 포함)
// =====================================

    public Itinerary saveOrUpdateItinerary(ItineraryTotalCreateRequestDTO requestDto) {
        // 1. Itinerary 저장 또는 수정
        Itinerary itinerary = saveOrUpdateItineraryDetails(requestDto.getItinerary());

//        // 2. 일정별 하루 계획 저장 (ItineraryPerDays)
//        saveOrUpdateItineraryPerDays(itinerary, requestDto.getItineraryPerDays());
//
//        // 3. 일정 내 이벤트 처리 (추가, 수정, 삭제)
//        saveOrUpdateItineraryEvents(itinerary, requestDto.getItineraryEvents());

        return itinerary;
    }

    private Itinerary saveOrUpdateItineraryDetails(ItineraryDTO dto) {
        Itinerary itinerary;
        if (dto.getId() != null) {
            itinerary = itineraryRepository.findById(dto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Itinerary not found with id " + dto.getId()));
            itinerary.updateFromDto(dto); // 변경된 정보 업데이트
        } else {
            itinerary = dto.toEntity();
        }
        return itineraryRepository.save(itinerary);
    }
//
//    private void saveOrUpdateItineraryPerDays(Itinerary itinerary, List<ItineraryPerDayDto> perDayDtos) {
//        if (perDayDtos == null) return;
//
//        // 기존 하루 일정 리스트 가져오기
//        List<ItineraryPerDay> existingDays = itinerary.getItineraryPerDays();
//
//        // 기존 ID 리스트 추출
//        Set<Long> existingIds = existingDays.stream()
//                .map(ItineraryPerDay::getId)
//                .collect(Collectors.toSet());
//
//        List<ItineraryPerDay> updatedDays = new ArrayList<>();
//        for (ItineraryPerDayDto dto : perDayDtos) {
//            if (dto.getId() != null && existingIds.contains(dto.getId())) {
//                // 기존 데이터 수정
//                ItineraryPerDay existingDay = existingDays.stream()
//                        .filter(day -> day.getId().equals(dto.getId()))
//                        .findFirst()
//                        .orElseThrow(() -> new EntityNotFoundException("Day not found"));
//                existingDay.updateFromDto(dto);
//                updatedDays.add(existingDay);
//                existingIds.remove(dto.getId());
//            } else {
//                // 새 데이터 추가
//                updatedDays.add(new ItineraryPerDay(dto, itinerary));
//            }
//        }
//
//        // 삭제된 일정 제거
//        List<ItineraryPerDay> toDelete = existingDays.stream()
//                .filter(day -> existingIds.contains(day.getId()))
//                .collect(Collectors.toList());
//        itinerary.getItineraryPerDays().removeAll(toDelete);
//
//        itinerary.getItineraryPerDays().addAll(updatedDays);
//    }
//
//    private void saveOrUpdateItineraryEvents(Itinerary itinerary, List<ItineraryEventDto> eventDtos) {
//        if (eventDtos == null) return;
//
//        List<ItineraryEvent> existingEvents = itinerary.getItineraryEvents();
//        Set<Long> existingIds = existingEvents.stream()
//                .map(ItineraryEvent::getId)
//                .collect(Collectors.toSet());
//
//        List<ItineraryEvent> updatedEvents = new ArrayList<>();
//        for (ItineraryEventDto dto : eventDtos) {
//            if (dto.getId() != null && existingIds.contains(dto.getId())) {
//                // 기존 이벤트 수정
//                ItineraryEvent existingEvent = existingEvents.stream()
//                        .filter(event -> event.getId().equals(dto.getId()))
//                        .findFirst()
//                        .orElseThrow(() -> new EntityNotFoundException("Event not found"));
//                existingEvent.updateFromDto(dto);
//                updatedEvents.add(existingEvent);
//                existingIds.remove(dto.getId());
//            } else {
//                // 새 이벤트 추가
//                Place place = placeRepository.findByGooglePlaceId(dto.getPlaceDTO().getGooglePlaceId())
//                        .orElseGet(() -> placeRepository.save(new Place(dto.getPlaceDTO())));
//                updatedEvents.add(new ItineraryEvent(dto, itinerary, place));
//            }
//        }
//
//        // 삭제된 이벤트 제거
//        List<ItineraryEvent> toDelete = existingEvents.stream()
//                .filter(event -> existingIds.contains(event.getId()))
//                .collect(Collectors.toList());
//        itineraryEventRepository.deleteAll(toDelete);
//
//        itinerary.getItineraryEvents().clear();
//        itinerary.getItineraryEvents().addAll(updatedEvents);
//    }



}
