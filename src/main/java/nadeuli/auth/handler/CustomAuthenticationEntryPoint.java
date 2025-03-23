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
                case UNEXPECTED_TOKEN -> "/error/500";
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

//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response,
//                         AuthenticationException authException) throws IOException {
//        log.warn("\uD83D\uDD12 {} commence", CustomAuthenticationEntryPoint.class.getName());
//        String accept = request.getHeader("Accept");
//        log.warn("🔒 인증 실패: 요청 Accept: {}", accept);
//
//        if (accept != null && accept.contains("text/html")) {
//            log.warn("🔒 VIEW : {}", accept);
//            // View 요청 → Refresh 처리 페이지로 리디렉트
//
//        } else {
//            log.warn("🔒 REST: {}", accept);
//            // API 요청 → JSON 응답
//
//        }
//    }
    //            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write("{\"success\": false, \"message\": \"Access Token Expired\"}");



//            String originalUrl = request.getRequestURI();
//            String redirectUrl = "/auth/refresh?redirect=" + URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
//            response.sendRedirect(redirectUrl);
    //        Integer errorCode = (Integer) request.getAttribute("exception");
//        if (errorCode == null) {
//            setResponse(response, ErrorCode.UNKNOWN_ERROR);
//        } else if (errorCode == ErrorCode.EXPIRED_TOKEN.getCode()) {
//            setResponse(response, ErrorCode.EXPIRED_TOKEN);
//        } else if (errorCode == ErrorCode.WRONG_TYPE_TOKEN.getCode()) {
//            setResponse(response, ErrorCode.WRONG_TYPE_TOKEN);
//        } // ... 기타 코드 분기
//    private void setResponse(HttpServletResponse response, ErrorCode code) throws IOException {
//        response.setContentType("application/json;charset=UTF-8");
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        JSONObject body = new JSONObject();
//        body.put("message", code.getMessage());
//        body.put("code", code.getCode());
//        response.getWriter().print(body);
//    }
}