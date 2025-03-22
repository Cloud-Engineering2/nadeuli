package nadeuli.config.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.internal.http2.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn(CustomAuthenticationEntryPoint.class.getName() + " commence");
        String accept = request.getHeader("Accept");
        log.warn("🔒 인증 실패: 요청 Accept: {}", accept);
        Integer errorCode = (Integer) request.getAttribute("exception");
//        if (errorCode == null) {
//            setResponse(response, ErrorCode.UNKNOWN_ERROR);
//        } else if (errorCode == ErrorCode.EXPIRED_TOKEN.getCode()) {
//            setResponse(response, ErrorCode.EXPIRED_TOKEN);
//        } else if (errorCode == ErrorCode.WRONG_TYPE_TOKEN.getCode()) {
//            setResponse(response, ErrorCode.WRONG_TYPE_TOKEN);
//        } // ... 기타 코드 분기
    }
//    private void setResponse(HttpServletResponse response, ErrorCode code) throws IOException {
//        response.setContentType("application/json;charset=UTF-8");
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        JSONObject body = new JSONObject();
//        body.put("message", code.getMessage());
//        body.put("code", code.getCode());
//        response.getWriter().print(body);
//    }
}