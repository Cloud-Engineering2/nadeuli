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
 *
 * ========================================================
 */

package nadeuli.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.Person;
import nadeuli.dto.request.BudgetRequestDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.entity.*;
import nadeuli.repository.*;
import nadeuli.service.ExpenseBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final ExpenseBookRepository expenseBookRepository;


    // 예산 설정  /api/itineraries/{iid}/budget
    @PutMapping("/{iid}/budget")
    public ResponseEntity<ExpenseBookDTO> updateBudget(@RequestBody @Valid BudgetRequestDTO budgetRequestDTO,
                                             @PathVariable("iid") Long iid) {
        Long budget = budgetRequestDTO.getTotalBudget();
        ExpenseBookDTO expenseBookDto = expenseBookService.setBudget(iid, budget);

        return ResponseEntity.ok(expenseBookDto);
    }



    // 총 예산, 지출, 잔액 조회
    @GetMapping("/{iid}/expenses") // url 필요...!
    public ResponseEntity<FinanceResponseDTO> getTotalExpense(@PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));

        ItineraryCollaborator itineraryCollaborator = itineraryCollaboratorRepository.findFirstByItinerary(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryCollaborator이 존재하지 않습니다"));

        String owner = itineraryCollaborator.getUser().getUserName();

        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);

        List<ItineraryEvent> itineraryEvents = itineraryPerDays.stream()
                .flatMap(itineraryPerDay -> itineraryEventRepository.findByItineraryPerDay(itineraryPerDay).stream())
                .collect(Collectors.toList());

        // 총 지출
        AtomicReference<Long> expenditure = new AtomicReference<>(0L);


        for (ItineraryEvent itineraryEvent : itineraryEvents) {
            // 정산 완료 후
            Map<String, Person> adjustment = expenseBookService.adjustmentExpense(itineraryEvent.getId());

            // UserName과 똑같은 TravelerName을 가진 Traveler 찾기

            Optional<Person> personOptional = Optional.ofNullable(adjustment.get(owner));

            personOptional.ifPresentOrElse(
                    person -> { // 존재할 때
                        Long total = person.getTotal();
                        Long totalSendedMoney = person.getSendedMoney() != null
                                ? person.getSendedMoney().values().stream().mapToLong(Long::longValue).sum()
                                : 0L;

                        Long totalReceivedMoney = person.getReceivedMoney() != null
                                ? person.getReceivedMoney().values().stream().mapToLong(Long::longValue).sum()
                                : 0L;

                        expenditure.updateAndGet(v -> v + (total + totalSendedMoney - totalReceivedMoney));
                    },
                    () -> { // 존재하지 않을 때
                    }
            );

        }
        Long totalExpense = expenditure.get();
        expenseBook.updateExpenses(totalExpense);

        // 총 예산
        Long budget = expenseBook.getTotalBudget();

        // 총 잔액
        Long balance = budget - totalExpense;


        return ResponseEntity.ok(new FinanceResponseDTO(null, budget, 0L, totalExpense, balance));
    }


    // ItineraryEvent 별 정산
    @GetMapping("/{iid}/events/{ieid}/adjustment")
    public ResponseEntity<FinanceResponseDTO> getAdjustment(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {
        Long itineraryEventId = Long.valueOf(ieid);
        Long itineraryId = Long.valueOf(iid);

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Itinerary가 존재하지 않습니다"));

        ExpenseBook expenseBook = expenseBookRepository.findByIid(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ExpenseBook이 존재하지 않습니다"));

        ItineraryCollaborator itineraryCollaborator = itineraryCollaboratorRepository.findFirstByItinerary(itinerary)
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryCollaborator가 존재하지 않습니다"));


        String owner = itineraryCollaborator.getUser().getUserName();
        System.out.println("내 이름");
        System.out.println(owner);

        List<ItineraryPerDay> itineraryPerDays = itineraryPerDayRepository.findByItinerary(itinerary);

        List<ItineraryEvent> itineraryEvents = itineraryPerDays.stream()
                .flatMap(itineraryPerDay -> itineraryEventRepository.findByItineraryPerDay(itineraryPerDay).stream())
                .collect(Collectors.toList());

        AtomicReference<Long> expenditure = new AtomicReference<>(0L);
        AtomicReference<Long> currentExpenditure = new AtomicReference<>(0L);

        Map<String, Person> response = new HashMap<>();

        for (ItineraryEvent event : itineraryEvents) {
            if (event.getId() <= itineraryEventId) {
                // 1/n 정산
                Map<String, Person> adjustment = expenseBookService.adjustmentExpense(event.getId());

                // UserName과 똑같은 정산자 찾기
                Optional<Person> personOptional = Optional.ofNullable(adjustment.get(owner));

                personOptional.ifPresentOrElse(
                        person -> { // 존재할 때
                            Long total = person.getTotal();
                            Long totalSendedMoney = person.getSendedMoney() != null
                                    ? person.getSendedMoney().values().stream().mapToLong(Long::longValue).sum()
                                    : 0L;

                            Long totalReceivedMoney = person.getReceivedMoney() != null
                                    ? person.getReceivedMoney().values().stream().mapToLong(Long::longValue).sum()
                                    : 0L;

                            expenditure.updateAndGet(v -> v + (total + totalSendedMoney - totalReceivedMoney));
                            currentExpenditure.updateAndGet(v -> total + totalSendedMoney - totalReceivedMoney);
                        },
                        () -> { // 존재하지 않을 때
                        }
                );

                response = adjustment;

            }
        }

        // 총 지출
        Long totalExpense = expenditure.get();
        expenseBook.updateExpenses(totalExpense);

        // 현재 Itinerary Event 지출
        Long currentExpense = currentExpenditure.get();

        // 총 예산
        Long budget = expenseBook.getTotalBudget();

        // 총 잔액
        Long balance = budget - totalExpense;


        return ResponseEntity.ok(new FinanceResponseDTO(response, budget, currentExpense, totalExpense, balance));
    }
}
