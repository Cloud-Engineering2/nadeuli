/* JournalController.java
 * nadeuli Service - 여행
 * 기행문 관련 Controller
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : JournalController
 * 이홍비    2025.02.26     GetMapping 쪽 수정
 * 이홍비    2025.02.28     RestController 와 Controller 로 구분
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.JournalDTO;
import nadeuli.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RequestMapping("/itineraries/{iid}/events/{ieid}/journal")
@Controller
public class JournalController {
    private final JournalService journalService;

    /*
    * 있어야 하는 거
    * 1. 해당 일정의 모든 기행 조회 => 최종 결과물 출력할 때 필요
    * 2. 기행 상세 보기
    * 3. 기행 (글 + 사진) 조회
    * 3-1. 글 crud
    * 3-2. 사진 crud
    *
    * */

    // 기행문 조회 (열람)
    @GetMapping()
    public String getJournal(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal";
    }

    @GetMapping("2")
    public String getJournal2(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal2";
    }

    // 기행문 조회 (열람)
    @GetMapping("3")
    public String getJournal3(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal3";
    }

    // 기행문 조회 (열람)
    @GetMapping("4")
    public String getJournal4(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal4";
    }

}
