/* ExpenseBookService.java
 * ExpenseBook 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   예산 산정 메서드 추가
 * 이홍비    2025.03.10   ExpenseBookDTO 반환 함수 구현
 * 고민정    2025.03.11   예산 산정 메서드 삭제
 * 고민정    2025.03.11   1/n 정산 메서드 수정
 * 이홍비    2025.03.13   고민정 작성 ExpenseBookController - getFinalAdjustment() 내부 로직
 *                       Service 에 추가
 * ========================================================
 */

package nadeuli.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.Person;
import nadeuli.dto.response.AdjustmentResponseDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.entity.*;
import nadeuli.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseBookService {
    private final ExpenseBookRepository expenseBookRepository;
    private final ItineraryRepository itineraryRepository;
    private final WithWhomRepository withWhomRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final TravelerRepository travelerRepository;
    private final ItineraryEventService itineraryEventService;

    // ExpenseBook Id 조회 by Itinerary
    public Long get(Long iid) {
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary 존재하지 않습니다"));
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));
        return expenseBook.getId();
    }



    // 1/n 정산
    public FinanceResponseDTO calculateMoney(Long itineraryId, Long itineraryEventId) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        // 모든 Traveler 조회
        List<Traveler> travelerList = travelerRepository.findAllByIid(itinerary);
        // Itinerary Event 조회
        ItineraryEvent itineraryEvent = itineraryEventRepository.findById(itineraryEventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryEvent가 존재하지 않습니다"));
        // 모든 ExpenseItem 조회
        List<ExpenseItem> expenseItemList = expenseItemRepository.findAllByIeid(itineraryEvent);

        // 1/n 정산 결과 변수
        Map<String, Person> persons = new HashMap<>();
        // 현재 itinerary event의 총 지출
        Long currentItineraryEventTotalExpense = 0L;
        // 개별 총 지출 계산 변수
        Map<String, Long> eachExpenses = new HashMap<>();

        // 계산
        for (ExpenseItem expenseItem : expenseItemList) {
            // WithWhom 이름 리스트
            List<WithWhom> withWhoms = withWhomRepository.findAllByEmid(expenseItem);
            List<String> withWhomNames = withWhoms.stream()
                    .map(whom -> whom.getTid().getTravelerName()).collect(Collectors.toList());

            // 비용
            Long cost = expenseItem.getExpense();
            Long adjustmentPayerCost = cost / (withWhoms.size() + 1);
            Long adjustmentRoundCost = Math.round((double) cost / (withWhoms.size()+1)); // 지불해야 할 돈은 '올림' (반올림x)

            // payer
            String payerName = expenseItem.getPayer().getTravelerName();
            Person payer = persons.getOrDefault(payerName, new Person());
            Long payerExpense = eachExpenses.getOrDefault(payerName, 0L);
            eachExpenses.put(payerName, payerExpense + adjustmentPayerCost);

            for (String with : withWhomNames) {

                if (with.equals(payerName)) {   // payerName == with인 경우 skip
                    continue;
                }

                Person withWhomPerson = persons.getOrDefault(with, new Person());
                Long withWhomoExpense = eachExpenses.getOrDefault(with, 0L);
                eachExpenses.put(with, withWhomoExpense + adjustmentRoundCost);
                payer.receive(with, adjustmentRoundCost);
                persons.put(payerName, payer);
                withWhomPerson.send(payerName, adjustmentRoundCost);
                persons.put(with, withWhomPerson);
            }


            currentItineraryEventTotalExpense += expenseItem.getExpense();
        }


        return new FinanceResponseDTO(persons, currentItineraryEventTotalExpense, eachExpenses);
    }



    // 최종 정산 정보 조회
    public FinanceResponseDTO getAdjustment(Long itineraryId) {
        // 1. Itinerary로 모든 ItineraryEvent 조회하기
        List<ItineraryEvent> itineraryEventList = itineraryEventService.getAllByItineraryId(itineraryId);

        // 2. 변수
        Long totalExpense = 0L;   // 여행 총 지출
        Map<String, Long> eachExpense = new HashMap<>(); // 여행 개별 총 지출
        Map<String, Person> totalAdjustment = new HashMap<>(); // 최종 1/n 정산

        // 3. 최종 정산
        for (ItineraryEvent event : itineraryEventList) {
            FinanceResponseDTO response = calculateMoney(itineraryId, event.getId());
            // 여행 총 지출
            totalExpense += response.getTotalExpense();
            // 여행 개별 총 지출
            for (Map.Entry<String, Long> entry : response.getEachExpenses().entrySet()) {
                eachExpense.put(entry.getKey(), eachExpense.getOrDefault(entry.getKey(), 0L) + entry.getValue());
            }
            // 1/n 정산
            Map<String, Person> adjustment = response.getAdjustment();

            for (String personName : adjustment.keySet()) {
                Person currentPerson = totalAdjustment.getOrDefault(personName, new Person()); // 기존 값 or 새 Person 객체

                Person newPerson = adjustment.get(personName);

                // sendedMoney 업데이트
                for (Map.Entry<String, Long> entry : newPerson.getSendedMoney().entrySet()) {
                    currentPerson.send(entry.getKey(), entry.getValue());
                }

                // receivedMoney 업데이트
                for (Map.Entry<String, Long> entry : newPerson.getReceivedMoney().entrySet()) {
                    currentPerson.receive(entry.getKey(), entry.getValue());
                }

                // 업데이트된 currentPerson을 다시 totalAdjustment에 저장
                totalAdjustment.put(personName, currentPerson);
            }
        }

        // 4. Traveler 지출 설정
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        List<Traveler> travelerList = travelerRepository.findAllByIid(itinerary);
        for (Traveler traveler : travelerList) {
            traveler.updateTotalExpense(eachExpense.get(traveler.getTravelerName()));
            System.out.println(traveler.getTotalExpense());
        }

        return new FinanceResponseDTO(totalAdjustment, totalExpense, eachExpense);
    }

    public ExpenseBookDTO updateExpenseBook(Long itineraryId, Long totalExpense) {
        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다."));
        ExpenseBookDTO expenseBookDto = getExpenseBook(itineraryId);
        ExpenseBook expenseBook = expenseBookDto.toEntity(itinerary);

        expenseBook.updateExpense(totalExpense);
        ExpenseBookDTO responseDto = ExpenseBookDTO.from(expenseBook);

        return responseDto;

    }



    // ExpenseBookDTO 반환
    public ExpenseBookDTO getExpenseBook(Long iid) {
        // 일정 조회
        Itinerary itinerary = itineraryRepository.findById(iid)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 존재하지 않습니다."));

        // 장부 조회
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 장부가 존재하지 않습니다."));

        return ExpenseBookDTO.from(expenseBook); // DTO 로 반환
    }


    // ExpenseBookController 에 있던 getFinalAdjustment 로직 옮김
    public AdjustmentResponseDTO getFinalSettlement(Long iid) {
        FinanceResponseDTO financeResponseDTO = getAdjustment(iid);
        Long totalExpense = financeResponseDTO.getTotalExpense();

        // 지출, 잔액 갱신
        ExpenseBookDTO expenseBookDto = updateExpenseBook(iid, totalExpense); // 지출
        Long balance = expenseBookDto.getTotalBudget() - totalExpense; // 잔액

        return new AdjustmentResponseDTO(financeResponseDTO.getAdjustment(),
                financeResponseDTO.getEachExpenses(),
                expenseBookDto,
                balance);
    }


}