/* EmailNotificationService.java
 * 이메일/푸시 알림 전송 API 추가
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-07       EmailNotificationService 초안
 * ========================================================
 */

package nadeuli.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true", matchIfMissing = true)  // ✅ 이메일 기능 활성화 시 서비스 로드
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * ✅ 5개월 이상 로그인하지 않은 사용자에게 이메일 알림 전송
     * - 매일 자정(00:00)마다 실행 (Scheduled Task)
     */
    @Scheduled(cron = "0 0 0 * * ?")  // 매일 자정 실행
    @Transactional
    public int notifyInactiveUsers() {
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(5);
        List<User> inactiveUsers = userRepository.findInactiveUsersSince(fiveMonthsAgo);

        if (inactiveUsers.isEmpty()) {
            log.info("✅ [EmailNotification] 모든 사용자가 정상적으로 로그인 중.");
            return 0;
        }

        int notifiedCount = 0;
        for (User user : inactiveUsers) {
            boolean sent = sendEmail(user.getUserEmail(), "🔔 장기간 미접속 알림",
                    "안녕하세요, " + user.getUserName() + "님!\n\n"
                            + "5개월 동안 로그인하지 않으셨습니다. 로그인 후 다시 활동을 시작해 주세요!\n\n"
                            + "👉 [로그인하기](https://your-website.com/login)\n\n"
                            + "감사합니다.\n"
                            + "- 나들이 팀 -");
            if (sent) notifiedCount++;
        }
        log.info("✅ [EmailNotification] {}명의 비활성 사용자에게 이메일 알림 전송 완료.", notifiedCount);
        return notifiedCount;
    }

    /**
     * ✅ Refresh Token 만료 30일 전에 사용자에게 이메일 알림 전송
     * - 매일 자정(00:00)마다 실행 (Scheduled Task)
     */
    @Scheduled(cron = "0 0 0 * * ?")  // 매일 자정 실행
    @Transactional
    public int notifyRefreshTokenExpiry() {
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        List<User> usersWithExpiringTokens = userRepository.findUsersWithExpiringRefreshTokens(thirtyDaysFromNow);

        if (usersWithExpiringTokens.isEmpty()) {
            log.info("✅ [EmailNotification] Refresh Token이 곧 만료될 사용자가 없습니다.");
            return 0;
        }

        int notifiedCount = 0;
        for (User user : usersWithExpiringTokens) {
            boolean sent = sendEmail(user.getUserEmail(), "🔔 Refresh Token 만료 예정",
                    "안녕하세요, " + user.getUserName() + "님!\n\n"
                            + "귀하의 Refresh Token이 30일 후 만료될 예정입니다. 로그인을 다시 진행하여 새롭게 갱신해 주세요.\n\n"
                            + "👉 [로그인하기](https://your-website.com/login)\n\n"
                            + "감사합니다.\n"
                            + "- 나들이 팀 -");
            if (sent) notifiedCount++;
        }
        log.info("✅ [EmailNotification] {}명의 사용자에게 Refresh Token 만료 알림 전송 완료.", notifiedCount);
        return notifiedCount;
    }

    /**
     * ✅ 이메일 발송 기능 (성공 여부 반환)
     */
    private boolean sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("✅ [EmailNotification] 이메일 전송 완료 - 받는 사람: {}", to);
            return true;
        } catch (MessagingException e) {
            log.error("🚨 [EmailNotification] 이메일 전송 실패 - 받는 사람: {}, 오류: {}", to, e.getLocalizedMessage());
            return false;
        }
    }
}
