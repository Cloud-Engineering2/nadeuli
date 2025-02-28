/* WithWhomController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   Controller 생성
 * 고민정    2025.02.28   WithWhom CRUD 메서드 추가
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.WithWhomDTO;
import nadeuli.service.TravelerService;
import nadeuli.service.WithWhomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class WithWhomController {

    private final WithWhomService withWhomService;
    private final TravelerService travelerService;

    // WithWhom 추가
    @PostMapping("/{iid}/expense/{emid}/withWhom")
    public ResponseEntity<Void> createWithWhom(@PathVariable("iid") Integer iid, @PathVariable("emid") Integer emid, @RequestParam List<String> withWhomNames) {
        Long itineraryId = Long.valueOf(iid);
        Long expenseItemId = Long.valueOf(emid);

        // Traveler 조회
        List<TravelerDTO> travelers = travelerService.getIds(itineraryId, withWhomNames);

        // WithWhom 생성
        withWhomService.addWithWhom(itineraryId, expenseItemId, travelers);
        return ResponseEntity.ok().build();
    }


    // WithWhom 삭제
    @DeleteMapping("/{iid}/expense/{emid}/withWhom/{wid}")
    public ResponseEntity<Void> deleteWithWhom(@PathVariable("iid") Integer iid, @PathVariable("emid") Integer emid, @PathVariable("wid") Integer wid) {
        withWhomService.cancelWithWhom(wid);
        return ResponseEntity.ok().build();
    }


    // WithWhom 조회
    @DeleteMapping("/{iid}/expense/{emid}/withWhom")
    public ResponseEntity<List<WithWhomDTO>> getWithWhom(@PathVariable("iid") Integer iid, @PathVariable("emid") Integer emid) {
        Long expenseItemId = Long.valueOf(emid);

        List<WithWhomDTO> withWhoms = withWhomService.listWitWhoms(expenseItemId);

        return ResponseEntity.ok().build();
    }
}