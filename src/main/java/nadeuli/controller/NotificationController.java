/* NotificationController.java
 * 이메일/푸시 알림 전송 API 추가
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-07       RequiredArgsConstructor 초안
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.service.EmailNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true", matchIfMissing = true)  // ✅ 이메일 기능이 활성화된 경우에만 컨트롤러 로드
public class NotificationController {

    private final EmailNotificationService emailNotificationService;

    /**
     * ✅ 5개월 이상 로그인하지 않은 사용자에게 이메일 알림 전송
     */
    @PostMapping("/inactive-users")
    public ResponseEntity<Map<String, Object>> notifyInactiveUsers() {
        int notifiedUsers = emailNotificationService.notifyInactiveUsers();
        log.info("✅ [Notification] {}명의 비활성 사용자에게 이메일 알림을 전송했습니다.", notifiedUsers);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ 비활성 사용자에게 이메일 알림 전송 완료",
                "notifiedUsers", notifiedUsers
        ));
    }

    /**
     * ✅ Refresh Token 만료 30일 전에 사용자에게 이메일 알림 전송
     */
    @PostMapping("/refresh-expiry")
    public ResponseEntity<Map<String, Object>> notifyRefreshTokenExpiry() {
        int notifiedUsers = emailNotificationService.notifyRefreshTokenExpiry();
        log.info("✅ [Notification] {}명의 사용자에게 Refresh Token 만료 알림을 전송했습니다.", notifiedUsers);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ Refresh Token 만료 알림 전송 완료",
                "notifiedUsers", notifiedUsers
        ));
    }
}

