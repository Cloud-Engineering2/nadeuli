package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "share_token")
public class ShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 32, unique = true)
    private String uuid;  // 공유 토큰 (8~32자리)

    @Column(name = "iid", nullable = false)
    private Long itineraryId;  // 일정 ID (외래 키)

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;  // 만료 시간 (옵션)}

    public ShareToken(String uuid, Long itineraryId, int ttlMinutes) {
        this.uuid = uuid;
        this.itineraryId = itineraryId;
        this.expiredAt = (ttlMinutes > 0) ? LocalDateTime.now().plusMinutes(ttlMinutes) : null;
    }

    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(LocalDateTime.now());
    }
}