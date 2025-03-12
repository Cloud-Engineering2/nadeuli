/* ExpenseBookController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   예산 산정 메서드 추가
 * 고민정    2025.03.11   예산 산정 메서드 삭제
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.Person;
import nadeuli.dto.response.AdjustmentResponseDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.entity.*;
import nadeuli.repository.*;
import nadeuli.service.ExpenseBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final ExpenseBookRepository expenseBookRepository;
    private final TravelerRepository travelerRepository;

    // ItineraryEvent 별 정산 : 총 지출, 1/n 정산
    @GetMapping("/{iid}/events/{ieid}/adjustment")
    public ResponseEntity<FinanceResponseDTO> getAdjustment(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {
        // PathVariable
        Long itineraryId = Long.valueOf(iid);
        Long itineraryEventId = Long.valueOf(ieid);

        FinanceResponseDTO response = expenseBookService.calculateMoney(itineraryId, itineraryEventId);

        return ResponseEntity.ok(response);
    }


    // Total 예산/잔액/지출 조회 및 Traveler 별 최종 1/n 정산
    @GetMapping("/{iid}/expense")
    public ResponseEntity<AdjustmentResponseDTO> getTotalExpense(@PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);
        // 1. Itinerary로 모든 ItineraryEvent 조회하기

        // Itinerary 조회
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));
        // ItineraryPerDay 조회
        List<ItineraryPerDay> itineraryPerDayList = itineraryPerDayRepository.findByItinerary(itinerary); // findAll
        // 모든 ItineraryEvent 조회
        List<ItineraryEvent> itineraryEventList = itineraryEventRepository.findByItineraryPerDayIn(itineraryPerDayList);
        // ExpenseBook 조회
        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));


        // 2. 변수
        Long totalExpense = 0L;   // 여행 총 지출
        Map<String, Long> eachExpense = new HashMap<>(); // 여행 개별 총 지출
        Map<String, Person> totalAdjustment = new HashMap<>(); // 최종 1/n 정산

        // 3. 최종 정산
        for (ItineraryEvent event : itineraryEventList) {
            FinanceResponseDTO response = expenseBookService.calculateMoney(itineraryId, event.getId());
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
        expenseBook.updateExpense(totalExpense);
        Long balance = expenseBook.getTotalBudget() - totalExpense;
        ExpenseBookDTO expenseBookDTO = ExpenseBookDTO.from(expenseBook);

        return ResponseEntity.ok(new AdjustmentResponseDTO(totalAdjustment, eachExpense, expenseBookDTO, balance));
    }



}
