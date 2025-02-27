/* TravelerController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   Controller 생성
 *
 * ========================================================
 */

package nadeuli.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.TravelerRequest;
import nadeuli.dto.TravelerResponse;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itinerary")
@RequiredArgsConstructor
public class TravelerController {

    private final TravelerService travelerService;

    // 여행자 추가
    @PostMapping("/{iid}/traveler")
    public ResponseEntity<TravelerDTO> registerTraveler(@RequestBody @Valid TravelerRequest travelerRequest,
                                                @PathVariable("iid") Long iid,
                                                BindingResult bindingResult) {
        String travelerName = travelerRequest.getTravelerName();
        TravelerDTO travelerDto = TravelerDTO.of(iid, travelerName);
        travelerService.addTraveler(travelerDto);
        return ResponseEntity.ok().build();
    }

    // 여행자들 조회
    @GetMapping("/{iid}/travelers")
    public ResponseEntity<TravelerResponse> retrieveTravelers(@PathVariable("iid") Long iid) {
        List<TravelerDTO> travelers = travelerService.getTravelers(iid);

        TravelerResponse response = TravelerResponse.toResponse(travelers);

        return ResponseEntity.ok(response);
    }



}
