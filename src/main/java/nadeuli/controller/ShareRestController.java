package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.response.ItineraryStatusResponse;
import nadeuli.service.ShareService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareRestController {

    private final ShareService shareService;

    /**
     * 일정 공유 링크 생성 (POST /api/share/create)
     */
    @PostMapping("/create")
    public ResponseEntity<String> createShareLink(@RequestParam Long itineraryId) {
        Long userId = 1L; // 세션없어서 임시로
        try {
            String shareToken = shareService.createShareToken(itineraryId, userId);
            return ResponseEntity.ok(shareToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/token")
    public ResponseEntity<String> getShareToken(@RequestParam Long itineraryId) {
        Long userId = 1L; // 세션없어서 임시로
        try {
            String shareToken = shareService.getShareToken(itineraryId, userId);
            return ResponseEntity.ok(shareToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 공유 토큰 삭제 (DELETE /api/share/delete)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteShareToken(@RequestParam Long itineraryId) {
        Long userId = 1L; // 세션없어서 임시로
        try {
            shareService.deleteShareToken(itineraryId, userId);
            return ResponseEntity.ok("공유 링크가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * 공유 링크를 통해 Collaborator 등록 (POST /api/share/join)
     */
    @PostMapping("/join")
    public ResponseEntity<String> joinItinerary(@RequestParam String shareToken) {
        Long userId = 2L; // 세션없어서 임시로
        try {
            String message = shareService.joinItinerary(shareToken, userId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * OWNER가 특정 사용자를 Collaborator에서 삭제 또는 GUEST가 자기자신 삭제(DELETE /api/share/remove)
     */

    @DeleteMapping("/remove-mine")
    public ResponseEntity<String> removeCollaborator(@RequestParam Long itineraryId) {

        Long userId = 1L;
        Long targetUserId = 1L;

        try {
            String message = shareService.removeCollaborator(itineraryId, userId, targetUserId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeCollaborator(@RequestParam Long itineraryId,
                                                     @RequestParam Long targetUserId) {

        Long userId = 1L;
        try {
            String message = shareService.removeCollaborator(itineraryId, userId, targetUserId);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    /**
     * 일정(lid)의 공유 상태 & 게스트 존재 여부 조회 (GET /api/share/status)
     */
    @GetMapping("/status")
    public ResponseEntity<ItineraryStatusResponse> getItineraryStatus(@RequestParam Long itineraryId) {
        ItineraryStatusResponse response = shareService.getItineraryStatus(1L, itineraryId);
        return ResponseEntity.ok(response);
    }
}
