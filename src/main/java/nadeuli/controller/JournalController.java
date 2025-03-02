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
 * 이홍비    2025.03.01     => 다시 되돌림
 * 이홍비                   사진 변경, 글 수정 시 사용할 함수 결정
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
//@RequestMapping("/itineraries/{iid}/events/{ieid}/journal")
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
    @GetMapping("/itineraries/{iid}/events/{ieid}/journal")
    public String getJournal(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal";
    }

    @GetMapping("/itineraries/{iid}/events/{ieid}/journal2")
    public String getJournal2(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal2";
    }

    // 기행문 조회 (열람)
    @GetMapping("/itineraries/{iid}/events/{ieid}/journal3")
    public String getJournal3(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal3";
    }

    // 기행문 조회 (열람)
    @GetMapping("/itineraries/{iid}/events/{ieid}/journal4")
    public String getJournal4(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, ModelMap model) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

        model.addAttribute("journal", journalDTO);

        //return ResponseEntity.ok(journalDTO);

        return "journal/journal4";
    }

    // 기행문 조회 (열람)
    @GetMapping("/itineraries/{iid}/events/{ieid}/journal5")
    public String getJournal5(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);

//        model.addAttribute("journal", journalDTO);

//        return ResponseEntity.ok(journalDTO);

        return "journal/journal5";
    }

    // 기행문 조회 (열람)
    @ResponseBody
    @GetMapping("/api/itineraries/{iid}/events/{ieid}/journal")
    public ResponseEntity<JournalDTO> getJournal4(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {
        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 조회한 기행문 : " + journalDTO);
//        System.out.println("JournalDTO.getContent()" + journalDTO.getContent());
//        System.out.println("journalDTO.getImageUrl() : " + journalDTO.getImageUrl());
//        System.out.println("journalDTO.getImageUrl() == null : " + (journalDTO.getImageUrl() == null));
//        System.out.println("journalDTO.getImageUrl().equals(\"\") : " + (journalDTO.getImageUrl().equals("")));
//        System.out.println("journalDTO.getImageUrl().equals(\"null\") : " + (journalDTO.getImageUrl().equals("null")));
//        System.out.println("journalDTO.getImageUrl().isEmpty() : " + (journalDTO.getImageUrl().isEmpty()));


        return ResponseEntity.ok(journalDTO);
    }

    // 사진 등록
    @ResponseBody
    @PostMapping("/api/itineraries/{iid}/events/{ieid}/photo")
    public ResponseEntity<JournalDTO> uploadPhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @RequestParam("file") MultipartFile file) {
        // Front : FormData() 에 file 추가하여 post 

        JournalDTO journalDTO = journalService.uploadPhoto(ieid, file);

        System.out.println("📌 사진 등록한 기행문 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 사진 수정
    @ResponseBody
    @PutMapping("/api/itineraries/{iid}/events/{ieid}/photo")
    public ResponseEntity<JournalDTO> modifiedPhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @RequestParam("file") MultipartFile file) {
        JournalDTO journalDTO = journalService.modifiedPhotoVer1(ieid, file);

        System.out.println("📌 사진 수정한 기행문 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 사진 삭제
    @ResponseBody
    @DeleteMapping("/api/itineraries/{iid}/events/{ieid}/photo")
    public ResponseEntity<String> deletePhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {
        JournalDTO journalDTO = journalService.deletePhoto(ieid);

        System.out.println("📌 사진 삭제한 기행문 : " + journalDTO);

        return ResponseEntity.ok("사진 삭제 완료");
    }

    // 사진 등록 - test
    @ResponseBody
    @PostMapping("/api/itineraries/{iid}/events/{ieid}/photo-test")
    public ResponseEntity<JournalDTO> uploadPhotoTest(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @RequestParam("imageURL") String imageURL) {
        JournalDTO journalDTO = journalService.uploadPhotoTest(ieid, imageURL);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 작성
    @ResponseBody
    @PostMapping("/api/itineraries/{iid}/events/{ieid}/content")
    public ResponseEntity<JournalDTO> writeContent(@PathVariable Long iid, @PathVariable Long ieid, @RequestParam("content") String content) {
        JournalDTO journalDTO = journalService.writeContent(ieid, content);

        System.out.println("📌 기행문 작성 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 수정
    @ResponseBody
    @PutMapping("/api/itineraries/{iid}/events/{ieid}/content")
    public ResponseEntity<JournalDTO> modifiedContent(@PathVariable Long iid, @PathVariable Long ieid, @RequestParam("content") String content) {
        JournalDTO journalDTO = journalService.modifiedContentVer2(ieid, content);

        System.out.println("📌 기행문 수정 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 삭제
    @ResponseBody
    @DeleteMapping("/api/itineraries/{iid}/events/{ieid}/content")
    public ResponseEntity<JournalDTO> deleteContent(@PathVariable Long iid, @PathVariable Long ieid) {
        JournalDTO journalDTO = journalService.deleteContent(ieid);

        System.out.println("📌 기행문 삭제 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }


}
