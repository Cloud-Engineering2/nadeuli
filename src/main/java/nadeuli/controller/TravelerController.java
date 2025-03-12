/* TravelerController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   Controller 생성, 여행자 추가/조회 메서드 추가
 * 고민정    2025.02.27   여행자 삭제 메서드 추가
 * 고민정    2025.03.11   여행자 예산 수정 메서드 추가
 * ========================================================
 */

package nadeuli.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.request.TravelerBudgetRequestDTO;
import nadeuli.dto.request.TravelerRequestDTO;
import nadeuli.dto.response.TravelerResponseDTO;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itinerary")
@RequiredArgsConstructor
public class TravelerController {

    private final TravelerService travelerService;

    // 여행자 추가
    @PostMapping("/{iid}/traveler")
    public ResponseEntity<Void> registerTraveler(@RequestBody @Valid TravelerRequestDTO travelerRequestDTO,
                                                @PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);

        String travelerName = travelerRequestDTO.getTravelerName();
        Long budget = travelerRequestDTO.getTotalBudget();
        TravelerDTO travelerDto = TravelerDTO.of(itineraryId, travelerName, budget);
        travelerService.addTraveler(travelerDto);
        return ResponseEntity.ok().build();
    }

    // 여행자들 조회
    @GetMapping("/{iid}/travelers")
    public ResponseEntity<TravelerResponseDTO> retrieveTravelers(@PathVariable("iid") Integer iid) {

        Long itineraryId = Long.valueOf(iid);
        List<TravelerDTO> travelers = travelerService.listTravelers(itineraryId);

        TravelerResponseDTO response = TravelerResponseDTO.toResponse(travelers);

        return ResponseEntity.ok(response);
    }

    // 여행자 삭제
    @DeleteMapping("/{iid}/traveler/{travelerName}")
    public ResponseEntity<List<TravelerDTO>> deleteTraveler(@PathVariable("iid") Integer iid, @PathVariable("travelerName") String travelerName) {
        Long itineraryId = Long.valueOf(iid);
        List<TravelerDTO> travelerDtos = travelerService.deleteTraveler(itineraryId, travelerName);

        return ResponseEntity.ok(travelerDtos);
    }


    // 여행자 예산 수정
    @PutMapping("/{iid}/traveler/{travelerName}")
        public ResponseEntity<ExpenseBookDTO> changeBudget(@PathVariable("iid") Integer iid, @PathVariable("travelerName") String travelerName, @RequestBody @Valid TravelerBudgetRequestDTO travelerBudgetRequestDTO) {
        Long itineraryId = Long.valueOf(iid);
        Long change = travelerBudgetRequestDTO.getTotalBudget();

        ExpenseBookDTO expenseBookDto = travelerService.updateBudget(itineraryId, travelerName, change);
        return ResponseEntity.ok(expenseBookDto);

    }



}
