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
 * 고민정   2025.02.26   addTraveler 메서드 추가
 *
 * ========================================================
 */
package nadeuli.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.TravelerDTO;
import nadeuli.entity.Itinerary;
import nadeuli.entity.Traveler;
import nadeuli.repository.ItineraryRepository;
import nadeuli.repository.TravelerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelerService {
    private final TravelerRepository travelerRepository;
    private final ItineraryRepository itineraryRepository;

    // 여행자 추가
    @Transactional
    public void addTraveler(TravelerDTO travelerDto) {
        Long itineraryId = travelerDto.getItineraryId();
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        Traveler traveler = travelerDto.toEntity(itinerary);
        travelerRepository.save(traveler);
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

}
