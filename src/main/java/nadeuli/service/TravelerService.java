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
 * 고민정   2025.02.26   Traveler CRUD 메서드 추가
 * 고민정   2025.03.24   traveler 이름, 예산 업데이트 메서드
 * ========================================================
 */
package nadeuli.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.response.ExpenseTravelerDTO;
import nadeuli.entity.ExpenseBook;
import nadeuli.entity.Itinerary;
import nadeuli.entity.Traveler;
import nadeuli.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelerService {
    private final TravelerRepository travelerRepository;
    private final ItineraryRepository itineraryRepository;
    private final ExpenseBookRepository expenseBookRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final WithWhomRepository withWhomRepository;
    // 여행자 추가
    @Transactional
    public void addTraveler(TravelerDTO travelerDto) {
        Long itineraryId = travelerDto.getItineraryId();
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));


        // 이름 중복 체크
        List<Traveler> existingTravelers = travelerRepository.findByIid(itinerary);
        boolean isDuplicate = existingTravelers.stream()
                .anyMatch(t -> t.getTravelerName().equalsIgnoreCase(travelerDto.getTravelerName()));

        if (isDuplicate) {
            throw new IllegalArgumentException("이미 동일한 이름의 여행자가 존재합니다: " + travelerDto.getTravelerName());
        }

        // 여행자 생성
        Traveler traveler = travelerDto.toEntity(itinerary);
        travelerRepository.save(traveler);
    }

    // 여행자들 조회
    @Transactional
    public List<TravelerDTO> listTravelers(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        return travelerRepository.findByIid(itinerary)
                .stream()
                .map(TravelerDTO::from)
                .toList();
    }

    @Transactional
    public List<ExpenseTravelerDTO> listExpenseTravelers(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다."));

        // 모든 여행자 조회
        List<Traveler> travelers = travelerRepository.findByIid(itinerary);

        // 지불자 / 소비자 ID 리스트
        List<Integer> payerIds = expenseItemRepository.findPayerIdsByItinerary(itineraryId);
        List<Integer> consumerIds = withWhomRepository.findConsumerIdsByItinerary(itineraryId);

        // DTO 매핑
        return travelers.stream()
                .map(t -> new ExpenseTravelerDTO(
                        t.getId(),
                        t.getTotalBudget(),
                        t.getTotalExpense(),
                        t.getTravelerName(),
                        payerIds.contains(t.getId()),
                        consumerIds.contains(t.getId())
                ))
                .toList();

    }




    // 이름으로 조회
    @Transactional
    public TravelerDTO getByName(Long itineraryId, String travelerName) {
        Traveler traveler = travelerRepository.findTravelerByItineraryIdAndTravelerName(itineraryId, travelerName)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다"));
        return TravelerDTO.from(traveler);
    }

    // 일정에 있는 Traveler 조회
    @Transactional
    public List<TravelerDTO> getByNames(Long itineraryId, List<String> withWhomNames) {

        List<Traveler> travelers = travelerRepository.findByItineraryIdAndTravelerNames(itineraryId, withWhomNames);

        return travelers.stream()
                .map(TravelerDTO::from)
                .collect(Collectors.toList());
    }

    // Traveler 삭제
    @Transactional
    public List<TravelerDTO> deleteTraveler(Long itineraryId, Integer tid) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new EntityNotFoundException("해당 Itinerary가 존재하지 않습니다"));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new EntityNotFoundException("해당 Itinerary가 존재하지 않습니다"));
        // 삭제 대상 조회
        Traveler traveler = travelerRepository.findById(tid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다."));

        // 삭제 제한 조건 확인
        boolean hasWithWhom = withWhomRepository.existsByTid(traveler);
        if (hasWithWhom) {
            throw new IllegalStateException("해당 여행자는 함께한 인물(WithWhom)에 사용 중이므로 삭제할 수 없습니다.");
        }

        boolean hasExpenseItem = expenseItemRepository.existsByPayer(traveler);
        if (hasExpenseItem) {
            throw new IllegalStateException("해당 여행자는 지출 항목(ExpenseItem)에 사용 중이므로 삭제할 수 없습니다.");
        }


        // Expense Book 예산 조정
        expenseBook.updateBudget(expenseBook.getTotalBudget() - traveler.getTotalBudget());

        // 삭제 수행
        travelerRepository.delete(traveler);


        // 남아 있는 여행자 리스트 변환 후 반환
        List<Traveler> remainedTravelers = travelerRepository.findAllByIid(itinerary);
        return remainedTravelers.stream()
                                    .map(TravelerDTO::from)
                                    .collect(Collectors.toList());

    }

    // 예산 수정
    @Transactional
    public ExpenseBookDTO updateBudget(Long itineraryId, Integer tid, Long change) {

        Traveler traveler = travelerRepository.findById(tid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다."));
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다."));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다."));

        // 여행자 예산 수정
        Long beforeTravelerBudget = traveler.getTotalBudget();
        traveler.updateTotalBudget(change);

        // ExpenseBook 예산 수정
        Long beforeBudget = expenseBook.getTotalBudget();
        expenseBook.updateBudget(beforeBudget - beforeTravelerBudget + change);
        travelerRepository.save(traveler);
        expenseBookRepository.save(expenseBook);
        return ExpenseBookDTO.from(expenseBook);

    }

    public TravelerDTO updateName(Integer tid, String editedName) {
        Traveler traveler = travelerRepository.findById(tid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Traveler가 존재하지 않습니다."));

        Itinerary itinerary = traveler.getIid();

        // 이름 중복 체크 (본인 제외)
        List<Traveler> existingTravelers = travelerRepository.findByIid(itinerary);
        boolean isDuplicate = existingTravelers.stream()
                .anyMatch(t -> !t.getId().equals(tid) && t.getTravelerName().equalsIgnoreCase(editedName));

        if (isDuplicate) {
            throw new IllegalArgumentException("이미 동일한 이름의 여행자가 존재합니다: " + editedName);
        }

        traveler.updateName(editedName);
        travelerRepository.save(traveler);
        return TravelerDTO.from(traveler);
    }
}
