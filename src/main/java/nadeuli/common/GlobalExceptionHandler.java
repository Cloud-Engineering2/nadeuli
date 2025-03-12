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
 * 국경민      03-12       Deprecated API 제거 및 로그 처리 개선
 * ========================================================
 */

package nadeuli.common;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ✅ 찾을 수 없을 때 발생하는 예외
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementExceptions(NoSuchElementException e) {
        log.warn("🚨 [handleNoSuchElementExceptions] 데이터 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 자료를 찾을 수 없습니다: " + e.getMessage());
    }

    /**
     * ✅ 일반적인 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception e) {
        log.error("🚨 [handleAllExceptions] 시스템 오류 발생: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예기치 못한 오류가 발생했습니다: " + e.getMessage());
    }

    /**
     * ✅ 예상치 못한 상황일 때 (잘못된 요청)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        log.warn("🚨 [handleIllegalStateException] 잘못된 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    /**
     * ✅ 로그인 시 아이디 또는 비밀번호 틀렸을 때 (인증 실패)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("🚨 [handleBadCredentialsException] 인증 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 정보가 일치하지 않습니다.");
    }

    // 🔥 OAuth & JWT 관련 예외 처리 추가 🔥

    /**
     * ✅ OAuth 인증 실패 예외 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleOAuthSecurityException(SecurityException e) {
        log.warn("🚨 [handleOAuthSecurityException] OAuth 인증 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("OAuth 인증에 실패했습니다.");
    }

    /**
     * ✅ JWT 토큰 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<String> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("🚨 [handleExpiredJwtException] JWT 만료됨");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("JWT 토큰이 만료되었습니다. 다시 로그인해주세요.");
    }

    /**
     * ✅ JWT 서명 오류 예외 처리
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<String> handleJwtSignatureException(SignatureException e) {
        log.warn("🚨 [handleJwtSignatureException] 잘못된 JWT 서명");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 JWT 서명입니다.");
    }

    /**
     * ✅ 지원되지 않는 JWT 예외 처리
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<String> handleUnsupportedJwtException(UnsupportedJwtException e) {
        log.warn("🚨 [handleUnsupportedJwtException] 지원되지 않는 JWT 형식");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("지원되지 않는 JWT 형식입니다.");
    }

    /**
     * ✅ 잘못된 형식의 JWT 예외 처리
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<String> handleMalformedJwtException(MalformedJwtException e) {
        log.warn("🚨 [handleMalformedJwtException] JWT 형식 오류");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JWT 형식이 올바르지 않습니다.");
    }

    /**
     * ✅ JWT 관련 기타 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("🚨 [handleIllegalArgumentException] JWT 처리 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JWT 처리 중 오류가 발생했습니다.");
    }
}
