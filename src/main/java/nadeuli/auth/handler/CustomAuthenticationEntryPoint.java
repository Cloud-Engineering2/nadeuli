/* CustomAuthenticationEntryPoint.java
 * nadeuli Service - 여행
 * JWT 인증 실패 핸들링 클래스 (Global Security EntryPoint 처리)
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.23
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.23     최초 작성 : JWT 인증 실패 처리 로직 구현 (HTML/REST 분기)
 */

package nadeuli.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import nadeuli.common.enums.ErrorCode;
import nadeuli.common.util.JwtUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String accept = request.getHeader("Accept");
        ErrorCode errorCode = Optional.ofNullable(request.getAttribute(JwtUtils.EXCEPTION_ATTRIBUTE))
                .filter(ErrorCode.class::isInstance)
                .map(ErrorCode.class::cast)
                .orElse(ErrorCode.UNEXPECTED_TOKEN);

        log.warn("🔒 인증 실패: ErrorCode = {}, Accept = {}", errorCode.name(), accept);

        if (accept != null && accept.contains("text/html")) {
            // 👉 VIEW 요청: 브라우저 접근
            String redirectUrl = switch (errorCode) {
                case EXPIRED_TOKEN -> "/auth/refresh-view?redirect=" + URLEncoder.encode(request.getRequestURI(), "UTF-8");
                case INVALID_TOKEN -> "/login?error=invalidToken";
                case INVALID_TOKEN_SIGNATURE -> "/login?error=invalidSignature";
                case UNSUPPORTED_TOKEN -> "/login?error=unsupportedToken";
                case UNEXPECTED_TOKEN -> "/error";
            };
            response.sendRedirect(redirectUrl);

        } else {
            // 👉 REST 요청: API 클라이언트
            int status = switch (errorCode) {
                case EXPIRED_TOKEN, INVALID_TOKEN -> HttpServletResponse.SC_UNAUTHORIZED;
                case INVALID_TOKEN_SIGNATURE -> HttpServletResponse.SC_FORBIDDEN;
                case UNSUPPORTED_TOKEN -> HttpServletResponse.SC_BAD_REQUEST;
                case UNEXPECTED_TOKEN -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            };

            response.setStatus(status);
            response.setContentType("application/json; charset=UTF-8");

            // EXPIRED_TOKEN이면 프론트가 /auth/refresh-rest 호출 유도할 수 있도록 명시
            String recoveryHint = errorCode == ErrorCode.EXPIRED_TOKEN ? "/auth/refresh-rest" : null;

            String errorJson = String.format("""
                {
                    "success": false,
                    "errorCode": "%s",
                    "message": "%s"%s
                }
                """,
                    errorCode.getCode(),
                    errorCode.getDescription(),
                    recoveryHint != null ? String.format(",\n\"recoveryHint\": \"%s\"", recoveryHint) : ""
            );

            response.getWriter().write(errorJson);
        }
    }
}