package nadeuli.dto.response;

import java.time.LocalDateTime; /** 토큰 응답 DTO */
    public class TokenResponse {
        public final String token;
        public final LocalDateTime expiryAt;

        public TokenResponse(String token, LocalDateTime expiryAt) {
            this.token = token;
            this.expiryAt = expiryAt;
        }
    }
