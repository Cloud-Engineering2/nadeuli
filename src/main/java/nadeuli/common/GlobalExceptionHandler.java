/* GlobalExceptionHandler.java
 * nadeuli Service - 여행
 * 전역 예외 처리 클래스
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : GlobalExceptionHandler
 * 국경민    2025.03.04        OAuth 및 JWT 관련 예외 처리 추가
 * ========================================================
 */

/* GlobalExceptionHandler.java
 * 공통 예외 처리 핸들러 (OAuth 및 JWT 예외 포함)
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       기본 예외 처리 추가
 * 국경민      03-06       OAuth 및 JWT 관련 예외 처리 추가
 * ========================================================
 */

package nadeuli.common;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ✅ 찾을 수 없을 때 발생하는 예외
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementExceptions(NoSuchElementException e) {
        e.printStackTrace(); // 로그 출력
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 자료를 찾을 수 없습니다: " + e.getMessage());
    }

    /**
     * ✅ 일반적인 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception e) {
        e.printStackTrace(); // 로그 출력
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예기치 못한 오류가 발생했습니다: " + e.getMessage());
    }

    /**
     * ✅ 예상치 못한 상황일 때 (잘못된 요청)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 400 코드 반환
    }

    /**
     * ✅ 로그인 시 아이디 또는 비밀번호 틀렸을 때 (인증 실패)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 정보가 일치하지 않습니다: " + e.getMessage());
    }

    // 🔥 OAuth & JWT 관련 예외 처리 추가 🔥

    /**
     * ✅ OAuth 인증 실패 예외 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleOAuthSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("OAuth 인증에 실패했습니다: " + e.getMessage());
    }

    /**
     * ✅ JWT 토큰 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<String> handleExpiredJwtException(ExpiredJwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("JWT 토큰이 만료되었습니다. 다시 로그인해주세요.");
    }

    /**
     * ✅ JWT 서명 오류 예외 처리
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<String> handleJwtSignatureException(SignatureException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 JWT 서명입니다.");
    }

    /**
     * ✅ 지원되지 않는 JWT 예외 처리
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<String> handleUnsupportedJwtException(UnsupportedJwtException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("지원되지 않는 JWT 형식입니다.");
    }

    /**
     * ✅ 잘못된 형식의 JWT 예외 처리
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<String> handleMalformedJwtException(MalformedJwtException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JWT 형식이 올바르지 않습니다.");
    }

    /**
     * ✅ JWT 관련 기타 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JWT 처리 중 오류가 발생했습니다: " + e.getMessage());
    }
}

