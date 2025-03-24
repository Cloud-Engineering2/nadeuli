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
 * 박한철    2025.03.11     엔티티에 movingDistanceFromPrevPlace 추가로 인한 변경사항
 * 박한철    2025.03.12     Update 이후 createdMappings 또한 리턴하도록 수정
 * 박한철    2025.03.14     템플릿 생성시 itineraryRegion, ExpenseBook 도 추가하도록 수정
 * ========================================================
 */
package nadeuli.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryEventDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.ItineraryRegionDTO;
import nadeuli.dto.request.ItineraryCreateRequestDTO;
import nadeuli.dto.request.ItineraryEventUpdateDTO;
import nadeuli.dto.request.ItineraryTotalUpdateRequestDTO;
import nadeuli.dto.response.*;
import nadeuli.dto.response.SaveEventResult;
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
    private final RegionRepository regionRepository;
    private final ItineraryRegionRepository itineraryRegionRepository;
    private final ExpenseBookRepository expenseBookRepository;
    private final JournalRepository journalRepository;
    private final ShareTokenRepository shareTokenRepository;
    private final S3Service s3Service;
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

        //지역 매핑 저장
        List<Long> selectedRegionIds = requestDTO.getSelectedRegionIds();
        List<Region> regions = regionRepository.findAllById(selectedRegionIds);
        List<ItineraryRegion> itineraryRegions = regions.stream()
                .map(region -> new ItineraryRegion(savedItinerary, region))
                .collect(Collectors.toList());
        itineraryRegionRepository.saveAll(itineraryRegions);

        // ExpenseBook 자동 생성
        ExpenseBook expenseBook = ExpenseBook.of(savedItinerary, 0L, 0L);
        expenseBookRepository.save(expenseBook);

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
        Page<Object[]> results = itineraryRepository.findByUserIdWithRole(userId, pageable);

        List<Long> itineraryIds = results.stream()
                .map(row -> ((Itinerary) row[0]).getId())
                .distinct()
                .collect(Collectors.toList());

        //  Region을 FETCH JOIN한 ItineraryRegion 리스트
        List<ItineraryRegion> allRegions = itineraryRegionRepository.findByItineraryIdIn(itineraryIds);

        //  region name까지 포함된 DTO로 변환 후 Map 구성
        Map<Long, List<ItineraryRegionDTO>> regionsByItineraryId = allRegions.stream()
                .map(ItineraryRegionDTO::from)
                .collect(Collectors.groupingBy(ItineraryRegionDTO::getItineraryId));

        return results.map(row -> {
            Itinerary itinerary = (Itinerary) row[0];
            String role = (String) row[1];
            boolean isShared = (boolean) row[2];
            boolean hasGuest = (boolean) row[3];

            List<ItineraryRegionDTO> regions = regionsByItineraryId.getOrDefault(itinerary.getId(), List.of());

            return ItineraryResponseDTO.from(itinerary, role, isShared, hasGuest, regions);
        });
    }


//    public Page<ItineraryResponseDTO> getMyItineraries(Long userId, Pageable pageable) {
//        // 1. 사용자(userId)가 참여한 일정(Itinerary) 조회
//        Page<Object[]> results = itineraryRepository.findByUserIdWithRole(userId, pageable);
//
//        // 2. 결과 변환
//        return results.map(row -> {
//            Itinerary itinerary = (Itinerary) row[0];  // Itinerary 객체
//            String role = (String) row[1];  // Collaborator 역할 정보
//
//            return ItineraryResponseDTO.from(itinerary, role);
//        });
//    }

// ===========================
//  READ: 특정 일정 조회 - Events 포함
// ===========================

    public ItineraryTotalReadResponseDTO getItineraryTotal(Long itineraryId, Long userId) {
        // 1. 일정 조회 (없으면 예외 발생)
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + itineraryId));

        // 2. 해당 일정에 속한 현재 사용자의 Collaborator 정보 조회
        ItineraryCollaborator collaborator = itineraryCollaboratorRepository
                .findByUserIdAndItineraryId(userId, itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("이 일정에 대한 권한이 없습니다."));

        // 3. 해당 일정의 perDay 목록 조회
        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);

        // 4. 해당 일정의 perDay 목록을 이용하여 각 perDay의 이벤트 목록 조회
        List<ItineraryEvent> itineraryEvents = itineraryEventRepository.findByItineraryPerDayIn(itineraryPerDays);

        // 5. 지역 정보 조회 (Region join 포함)
        List<ItineraryRegion> regions = itineraryRegionRepository.findByItineraryIdWithRegion(itineraryId);
        List<ItineraryRegionDTO> regionDTOs = regions.stream()
                .map(ItineraryRegionDTO::from)
                .toList();


        // 5. DTO 변환 후 반환
        return new ItineraryTotalReadResponseDTO(
                ItineraryResponseDTO.from(collaborator),  // Collaborator를 기반으로 DTO 변환
                itineraryPerDays.stream()
                        .map(ItineraryPerDayDTO::from)
                        .toList(), // ItineraryPerDay -> ItineraryPerDaySimpleDTO 변환
                itineraryEvents.stream()
                        .map(ItineraryEventSimpleDTO::from)
                        .toList()  //  ItineraryEvent -> ItineraryEventSimpleDTO 변환
                ,regionDTOs
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
        SaveEventResult result = saveOrUpdateItineraryEvents(itineraryPerDays, requestDto.getItineraryEvents());

        // 업데이트된 일정 정보를 DTO로 반환
        // Response DTO로 변환 후 반환
        return new ItineraryTotalUpdateResponseDTO(
                ItineraryDTO.from(itinerary),
                itineraryPerDays.stream().map(ItineraryPerDayDTO::from).collect(Collectors.toList()),
                result.getEvents(),
                result.getCreatedMappings()
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
                        itinerary, dto.getDayCount(), dto.getStartTime(), dto.getDayOfWeek()
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


    public SaveEventResult saveOrUpdateItineraryEvents(List<ItineraryPerDay> existingDays,
                                                       List<ItineraryEventUpdateDTO> eventDtos) {
        if (eventDtos == null || eventDtos.isEmpty()) {
            return new SaveEventResult(Collections.emptyList(), Collections.emptyList());
        }

        // 기존 이벤트 조회
        List<ItineraryEvent> existingEvents = itineraryEventRepository.findByItineraryPerDayIn(existingDays);

        // 매핑
        Map<Integer, ItineraryPerDay> dayCountToEntityMap = existingDays.stream()
                .collect(Collectors.toMap(ItineraryPerDay::getDayCount, Function.identity()));

        Map<Long, ItineraryEvent> existingEventMap = existingEvents.stream()
                .collect(Collectors.toMap(ItineraryEvent::getId, Function.identity()));

        Map<Long, Place> placeCache = placeRepository.findAllById(
                eventDtos.stream().map(ItineraryEventUpdateDTO::getPid).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Place::getId, Function.identity()));

        List<ItineraryEvent> allEvents = new ArrayList<>();
        List<ItineraryEvent> newEvents = new ArrayList<>();
        List<CreatedEventMapping> createdMappings = new ArrayList<>();

        for (ItineraryEventUpdateDTO dto : eventDtos) {
            if (dto.getId() != null && existingEventMap.containsKey(dto.getId())) {
                // 기존 이벤트 수정
                ItineraryEvent existingEvent = existingEventMap.get(dto.getId());
                existingEvent.updateFromDto(dto, dayCountToEntityMap.get(dto.getDayCount()));
                allEvents.add(existingEvent);
                existingEventMap.remove(dto.getId()); // 남은 것은 삭제 대상으로 분류
            } else {
                // 신규 이벤트 생성
                Place place = placeCache.get(dto.getPid());
                if (place == null) {
                    throw new EntityNotFoundException("Place not found: " + dto.getPid());
                }

                ItineraryEvent newEvent = ItineraryEvent.of(
                        dayCountToEntityMap.get(dto.getDayCount()),
                        place,
                        dto.getStartMinuteSinceStartDay(),
                        dto.getEndMinuteSinceStartDay(),
                        dto.getMovingMinuteFromPrevPlace(),
                        dto.getMovingDistanceFromPrevPlace()
                );

                allEvents.add(newEvent);
                newEvents.add(newEvent);

                // hashId → eventId 매핑 준비 (eventId는 save 이후 할당)
                createdMappings.add(new CreatedEventMapping(dto.getHashId(), null));
            }
        }

        // 삭제 처리 (요청에서 누락된 기존 이벤트 → 삭제 대상)
        List<ItineraryEvent> toDelete = new ArrayList<>(existingEventMap.values());
        if (!toDelete.isEmpty()) {
            itineraryEventRepository.deleteAll(toDelete);
        }

        // 저장 (업데이트 + 신규 통합)
        itineraryEventRepository.saveAll(allEvents);

        // createdMappings ID 채우기
        for (int i = 0; i < newEvents.size(); i++) {
            CreatedEventMapping mapping = createdMappings.get(i);
            createdMappings.set(i, new CreatedEventMapping(mapping.getHashId(), newEvents.get(i).getId()));
        }
        // 최종 전체 이벤트 → DTO 변환
        List<ItineraryEventDTO> eventDtoList = allEvents.stream()
                .map(ItineraryEventDTO::from)
                .collect(Collectors.toList());

        return new SaveEventResult(eventDtoList, createdMappings);
    }


    @Transactional
    public void deleteItinerary(Long itineraryId, Long userId) {
        // 1. 일정 및 사용자 권한 확인
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + itineraryId));

        ItineraryCollaborator collaborator = itineraryCollaboratorRepository
                .findByUserIdAndItineraryId(userId, itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("이 일정에 대한 권한이 없습니다."));

        if (!Objects.equals(collaborator.getIcRole(), "ROLE_OWNER")) {
            throw new IllegalStateException("해당 일정 삭제 권한이 없습니다 (Owner만 삭제 가능).");
        }

        // 2. ItineraryPerDay → ItineraryEvent → Journal 조회 및 이미지 삭제
        List<ItineraryPerDay> perDays = itineraryPerDayRepository.findByItinerary(itinerary);
        List<ItineraryEvent> events = itineraryEventRepository.findByItineraryPerDayIn(perDays);

        List<Journal> journals = journalRepository.findByItineraryEvents(events);
        for (Journal journal : journals) {
            String imageUrl = journal.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                s3Service.deleteFile(imageUrl);
            }
        }
        journalRepository.deleteAll(journals);

        // 3. ItineraryRegion 삭제
        itineraryRegionRepository.deleteByItineraryId(itineraryId);

        // 4. ItineraryPerDay 삭제 → ItineraryEvent, ExpenseItem 자동 삭제
        itineraryPerDayRepository.deleteAllByItinerary(itinerary);

        // 5. Collaborator 삭제
        itineraryCollaboratorRepository.deleteByItinerary(itinerary);

        // 6. 공유토큰삭제
        shareTokenRepository.deleteByItineraryId(itineraryId);

        // 7. Itinerary 삭제 → ExpenseBook 자동 삭제 → ExpenseItem도 자동 삭제
        itineraryRepository.delete(itinerary);
    }

}
