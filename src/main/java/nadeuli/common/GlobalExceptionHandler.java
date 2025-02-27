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
 * ========================================================
 */

package nadeuli.common;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 찾을 수 없을 때
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementExceptions (NoSuchElementException e) {
        e.printStackTrace(); // 출력

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 자료를 찾을 수 없습니다. : " + e.getMessage());
    }

    // 일반적인 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception e) {
        e.printStackTrace(); // 출력
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예기치 못한 오류가 발생했습니다 : " + e.getMessage());
    }

    // 예상치 못한 상황일 때
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 잘못된 요청 - 400 코드 반환
    }

    // 로그인 시 아이디 or 비밀번호 틀렸을 때
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage()); // 인증 실패 - 401 코드 반환
    }
}
