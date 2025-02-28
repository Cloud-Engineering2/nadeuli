/* WithWhomService.java
 * WithWhom 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정   2025.02.28    C,R,D 메서드 추가
 *
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.WithWhomDTO;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.Itinerary;
import nadeuli.entity.WithWhom;
import nadeuli.repository.ExpenseItemRepository;
import nadeuli.repository.ItineraryRepository;
import nadeuli.repository.WithWhomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WithWhomService {
    private final WithWhomRepository withWhomRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final ItineraryRepository itineraryRepository;

    // WithWhom 추가
    public void addWithWhom(Long itineraryId, Long expenseItemId, List<TravelerDTO> travelerDtos) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        // ExpenseItem 조회
        ExpenseItem expenseItem = expenseItemRepository.findById(expenseItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseItem이 존재하지 않습니다"));

        // WithWhom Entity 생성 후 저장
        travelerDtos.stream()
                .map(travelerDto -> WithWhom.of(expenseItem, travelerDto.toEntity(itinerary)))
                .forEach(withWhomRepository::save);
    }

    // WithWhom 삭제
    public void cancelWithWhom(Integer withWhomId) {
        if (withWhomRepository.existsById(withWhomId)) {
            withWhomRepository.deleteById(withWhomId);
        }
    }

    public List<WithWhomDTO> listWitWhoms(Long expenseItemId) {
        ExpenseItem expenseItem = expenseItemRepository.findById(expenseItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseItem이 존재하지 않습니다"));
        List<WithWhom> withWhoms = withWhomRepository.findAllByEmid(expenseItem);
        return withWhoms.stream().map(WithWhomDTO::from).collect(Collectors.toList());
    }
}
