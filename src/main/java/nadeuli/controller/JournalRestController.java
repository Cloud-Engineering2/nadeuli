/* JournalRestController.java
 * nadeuli Service - 여행
 * 기행문 관련 Rest Controller
 * 작성자 : 이홍비
 * 최초 작성 일자 : 2025.02.25
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
 * 이홍비    2025.03.03     사진 파일 다운로드 추가 구현
 *                         불필요한 것 정리
 * 이홍비    2025.03.06     GetMapping 쪽 함수 이름 정리 + 내부 정리
 * 이홍비    2025.03.20     Controller 와 RestController 분리 : exception handler
 *                         로그인 인증 관련 처리
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.JournalDTO;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.ItineraryCollaboratorService;
import nadeuli.service.JournalService;
import nadeuli.service.S3Service;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/itineraries/{iid}/events/{ieid}")
public class JournalRestController {
    private final JournalService journalService;
    private final S3Service s3Service;
    private final ItineraryCollaboratorService itineraryCollaboratorService;

    /*
    * 있어야 하는 거
    * 1. 해당 일정의 모든 기행 조회 => 최종 결과물 출력할 때 필요
    * 2. 기행 상세 보기
    * 3. 기행 (글 + 사진) 조회
    * 3-1. 글 crud
    * 3-2. 사진 crud
    * 3-3. 사진 다운로드
    * */

    // 기행문 조회 (열람)
    @GetMapping("/journal")
    public ResponseEntity<JournalDTO> getJournalDTO(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.getJournal(ieid); // Jouranl 조회
        //JournalDTO journalDTO = journalService.getJournal(ieid, userDetails.getUser().getId());

        System.out.println("📌 조회한 기행문 : " + journalDTO);
//        System.out.println("JournalDTO.getContent()" + journalDTO.getContent());
//        System.out.println("journalDTO.getImageUrl() : " + journalDTO.getImageUrl());
//        System.out.println("journalDTO.getImageUrl() == null : " + (journalDTO.getImageUrl() == null));
//        System.out.println("journalDTO.getImageUrl().equals(\"\") : " + (journalDTO.getImageUrl().equals("")));
//        System.out.println("journalDTO.getImageUrl().equals(\"null\") : " + (journalDTO.getImageUrl().equals("null")));
//        System.out.println("journalDTO.getImageUrl().isEmpty() : " + (journalDTO.getImageUrl().isEmpty()));

        return ResponseEntity.ok(journalDTO);
    }

    // 사진 다운로드
    @GetMapping("/photo/download")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable("iid") long iid, @PathVariable("ieid") long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        ResponseEntity<Resource> file = s3Service.downloadFile(journalService.getJournal(ieid).getImageUrl());

        System.out.println("📌 사진 다운로드 : " + file);

        return file;
    }

    // 사진 등록
    @PostMapping("/photo")
    public ResponseEntity<JournalDTO> uploadPhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Front : FormData() 에 file 추가하여 post

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.uploadPhoto(ieid, file);

        System.out.println("📌 사진 등록한 기행문 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 사진 수정
    @PutMapping("/photo")
    public ResponseEntity<JournalDTO> modifiedPhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.modifiedPhotoVer1(ieid, file);

        System.out.println("📌 사진 수정한 기행문 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 사진 삭제
    @DeleteMapping("/photo")
    public ResponseEntity<JournalDTO> deletePhoto(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.deletePhoto(ieid);

        System.out.println("📌 사진 삭제한 기행문 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 작성
    @PostMapping("/content")
    public ResponseEntity<JournalDTO> writeContent(@PathVariable Long iid, @PathVariable Long ieid, @RequestParam("content") String content, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.writeContent(ieid, content);

        System.out.println("📌 기행문 작성 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 수정
    @PutMapping("/content")
    public ResponseEntity<JournalDTO> modifiedContent(@PathVariable Long iid, @PathVariable Long ieid, @RequestParam("content") String content, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.modifiedContentVer2(ieid, content);

        System.out.println("📌 기행문 수정 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }

    // 글 삭제
    @DeleteMapping("/content")
    public ResponseEntity<JournalDTO> deleteContent(@PathVariable Long iid, @PathVariable Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        JournalDTO journalDTO = journalService.deleteContent(ieid);

        System.out.println("📌 기행문 삭제 : " + journalDTO);

        return ResponseEntity.ok(journalDTO);
    }


}
