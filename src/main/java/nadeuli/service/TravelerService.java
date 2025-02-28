/* TravelerService.java
 * Traveler 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철   2025.02.25   Service 생성
 * 고민정   2025.02.26   addTraveler 메서드 추가
 *
 * ========================================================
 */
package nadeuli.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.TravelerDTO;
import nadeuli.entity.Itinerary;
import nadeuli.entity.Traveler;
import nadeuli.repository.ItineraryRepository;
import nadeuli.repository.TravelerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelerService {
    private final TravelerRepository travelerRepository;
    private final ItineraryRepository itineraryRepository;

    // 여행자 추가
    @Transactional
    public Integer addTraveler(TravelerDTO travelerDto) {
        Long itineraryId = travelerDto.getItineraryId();
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        Traveler traveler = travelerDto.toEntity(itinerary);
        travelerRepository.save(traveler);

        return traveler.getId();
    }

    // 여행자들 조회
    @Transactional
    public List<TravelerDTO> getTravelers(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        return travelerRepository.findByIid(itinerary)
                .stream()
                .map(TravelerDTO::from)
                .toList();
    }

    // 이름으로 조회
    public TravelerDTO get(String travelerName) {
        Traveler traveler = travelerRepository.findByTravelerName(travelerName)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다"));
        return TravelerDTO.from(traveler);
    }

    // 일정에 있는 Traveler 조회
    public List<TravelerDTO> getIds(Long itineraryId, List<String> withWhomNames) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        List<Traveler> travelers = travelerRepository.findByIidAndTravelerNameIn(itinerary, withWhomNames);

        return travelers.stream()
                .map(TravelerDTO::from)
                .collect(Collectors.toList());
    }


    public List<TravelerDTO> deleteTraveler(String travelerName, Long itineraryId) {
        List<Traveler> travelers = travelerRepository.findAllByIid(itineraryId);

        // 삭제할 대상 필터링
        Traveler deletion = travelers.stream()
                                        .filter(traveler -> traveler.getTravelerName().equals(travelerName))
                                        .findFirst()
                                        .orElseThrow(() -> new EntityNotFoundException("해당 Traveler가 존재하지 않습니다"));
        // 삭제 수행
        travelerRepository.delete(deletion);

        // 남아 있는 여행자 리스트 변환 후 반환
        List<Traveler> remainedTravelers = travelerRepository.findAllByIid(itineraryId);
        return remainedTravelers.stream()
                                    .map(TravelerDTO::from)
                                    .collect(Collectors.toList());

    }
}
