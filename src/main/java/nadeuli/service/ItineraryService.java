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
 * 박한철    2025.02.28     일정 전체 (itinerary, PerDay, Event) Update 기능 개발 완료
 * ========================================================
 */
package nadeuli.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryEventDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.request.ItineraryEventUpdateDTO;
import nadeuli.dto.request.ItineraryTotalUpdateRequestDTO;
import nadeuli.dto.response.*;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
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

    public ItineraryTotalReadResponseDTO getItineraryTotal(Long itineraryId, Long userId) {
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
        return new ItineraryTotalReadResponseDTO(
                ItineraryResponseDTO.from(collaborator),  // ✅ Collaborator를 기반으로 DTO 변환
                itineraryPerDays.stream()
                        .map(ItineraryPerDayDTO::from)
                        .toList(), // ✅ ItineraryPerDay -> ItineraryPerDaySimpleDTO 변환
                itineraryEvents.stream()
                        .map(ItineraryEventSimpleDTO::from)
                        .toList()  // ✅ ItineraryEvent -> ItineraryEventSimpleDTO 변환
        );
    }

// =====================================
//  CREATE & UPDATE: 특정 일정 저장 및 수정 ( Event 포함)
// =====================================

    @Transactional
    public ItineraryTotalUpdateResponseDTO saveOrUpdateItinerary(ItineraryTotalUpdateRequestDTO requestDto) {
        // 일정의 기본 정보를 저장 또는 업데이트
        Itinerary itinerary = saveOrUpdateItineraryDetails(requestDto.getItinerary());

        // 일정의 일별 정보를 저장 또는 업데이트
        List<ItineraryPerDay> itineraryPerDays = saveOrUpdateItineraryPerDays(itinerary, requestDto.getItineraryPerDays());

        // 일정의 이벤트 정보를 저장 또는 업데이트
        List<ItineraryEvent> itineraryEvents = saveOrUpdateItineraryEvents(itineraryPerDays, requestDto.getItineraryEvents());

        // 업데이트된 일정 정보를 DTO로 반환
        // Response DTO로 변환 후 반환
        return new ItineraryTotalUpdateResponseDTO(
                ItineraryDTO.from(itinerary),
                itineraryPerDays.stream().map(ItineraryPerDayDTO::from).collect(Collectors.toList()),
                itineraryEvents.stream().map(ItineraryEventDTO::from).collect(Collectors.toList())
        );
    }

    private Itinerary saveOrUpdateItineraryDetails(ItineraryDTO dto) {
        if (dto.getId() != null) {
            // 기존 일정이 존재하는 경우, 조회 후 업데이트 수행
            Itinerary itinerary = itineraryRepository.findById(dto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Itinerary not found with id " + dto.getId()));

            // 변경 사항이 없으면 기존 객체 반환
            if (!itinerary.hasChanges(dto)) {
                return itinerary;
            }

            // DTO 정보를 기존 일정에 반영 후 저장
            itinerary.updateFromDto(dto);
            return itineraryRepository.save(itinerary);
        }

        // 신규 일정인 경우, 엔티티 변환 후 저장
        return itineraryRepository.save(dto.toEntity());
    }

    private List<ItineraryPerDay> saveOrUpdateItineraryPerDays(Itinerary itinerary, List<ItineraryPerDayDTO> perDayDtos) {
        if (perDayDtos == null) return Collections.emptyList();

        // 기존 일정의 일별 데이터 조회
        List<ItineraryPerDay> existingDays = itineraryPerDayRepository.findByItinerary(itinerary);
        Map<Integer, ItineraryPerDay> existingDayMap = existingDays.stream()
                .collect(Collectors.toMap(ItineraryPerDay::getDayCount, Function.identity()));

        Set<Integer> existingDayCounts = new HashSet<>(existingDayMap.keySet());
        List<ItineraryPerDay> updatedDays = new ArrayList<>();

        for (ItineraryPerDayDTO dto : perDayDtos) {
            if (existingDayMap.containsKey(dto.getDayCount())) {
                // 기존 데이터가 존재하면 업데이트 수행
                ItineraryPerDay existingDay = existingDayMap.get(dto.getDayCount());
                existingDay.updateFromDto(dto);
                updatedDays.add(existingDay);
                existingDayCounts.remove(dto.getDayCount());
            } else {
                // 신규 데이터 추가
                updatedDays.add(ItineraryPerDay.of(
                        itinerary, dto.getDayCount(), dto.getStartTime(), dto.getEndTime(), dto.getDayOfWeek()
                ));
            }
        }

        // 삭제할 일정 일별 데이터 추출
        List<ItineraryPerDay> toDelete = existingDays.stream()
                .filter(day -> existingDayCounts.contains(day.getDayCount()))
                .collect(Collectors.toList());

        // 삭제 및 업데이트 수행
        if (!toDelete.isEmpty()) {
            itineraryPerDayRepository.deleteAllInBatch(toDelete);
        }
        if (!updatedDays.isEmpty()) {
            itineraryPerDayRepository.saveAll(updatedDays);
        }

        return itineraryPerDayRepository.findByItinerary(itinerary);
    }

    private List<ItineraryEvent> saveOrUpdateItineraryEvents(List<ItineraryPerDay> existingDays, List<ItineraryEventUpdateDTO> eventDtos) {
        if (eventDtos == null) return Collections.emptyList();

        // 일정과 관련된 기존 이벤트 및 일별 일정 데이터 조회
        List<ItineraryEvent> existingEvents = itineraryEventRepository.findByItineraryPerDayIn(existingDays);

        // 일별 일정 데이터를 Map 형태로 변환
        Map<Integer, ItineraryPerDay> dayCountToEntityMap = existingDays.stream()
                .collect(Collectors.toMap(ItineraryPerDay::getDayCount, Function.identity()));

        // 기존 이벤트 데이터를 Map 형태로 변환
        Map<Long, ItineraryEvent> existingEventMap = existingEvents.stream()
                .collect(Collectors.toMap(ItineraryEvent::getId, Function.identity()));

        // Place 객체를 한 번에 조회하여 캐싱
        Map<Long, Place> placeCache = placeRepository.findAllById(
                eventDtos.stream().map(ItineraryEventUpdateDTO::getPid).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Place::getId, Function.identity()));

        List<ItineraryEvent> updatedEvents = new ArrayList<>();

        for (ItineraryEventUpdateDTO dto : eventDtos) {
            if (dto.getId() != null && existingEventMap.containsKey(dto.getId())) {
                // 기존 이벤트가 존재하면 업데이트 수행
                ItineraryEvent existingEvent = existingEventMap.get(dto.getId());
                existingEvent.updateFromDto(dto, dayCountToEntityMap.get(dto.getDayCount()));
                updatedEvents.add(existingEvent);
                existingEventMap.remove(dto.getId());
            } else {
                // 새로운 이벤트 추가
                Place place = placeCache.get(dto.getPid());
                if (place == null) {
                    throw new EntityNotFoundException("Place not found with id " + dto.getPid());
                }
                updatedEvents.add(ItineraryEvent.of(
                        dayCountToEntityMap.get(dto.getDayCount()),
                        place,
                        dto.getStartMinuteSinceStartDay(),
                        dto.getEndMinuteSinceStartDay(),
                        dto.getMovingMinuteFromPrevPlace()
                ));
            }
        }

        // 삭제할 이벤트 리스트 추출
        List<ItineraryEvent> toDelete = new ArrayList<>(existingEventMap.values());

        // 삭제 및 업데이트 수행
        if (!toDelete.isEmpty()) {
            itineraryEventRepository.deleteAll(toDelete);
        }
        if (!updatedEvents.isEmpty()) {
            itineraryEventRepository.saveAll(updatedEvents);
        }

        return itineraryEventRepository.findByItineraryPerDayIn(existingDays);
    }





}
