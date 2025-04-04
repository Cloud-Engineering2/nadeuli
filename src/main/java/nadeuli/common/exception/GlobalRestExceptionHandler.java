/*
 * GlobalRestExceptionHandler.java
 * nadeuli Service - 여행
 * REST API 전용 전역 예외 처리 클래스 (특정 Controller 대상)
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.19     최초 작성 : REST 전용 예외 처리 및 Controller 대상 명시
 * 이홍비    2025.03.20     JournalRestController, TravelBottomLineRestController 추가
 * ========================================================
 */

package nadeuli.common.exception;

import jakarta.persistence.EntityNotFoundException;
import nadeuli.controller.*;
import nadeuli.dto.response.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = {
        AdminRestController.class,
        ExpenseItemRestController.class,
        GoogleApiController.class,
        GoogleMapPageController.class,
        IndexController.class,
        ItineraryRestController.class,
        JournalRestController.class,
        OAuthController.class,
        OpenAITravelController.class,
        PlaceController.class,
        RegionRestController.class,
        ShareRestController.class,
        TravelBottomLineRestController.class,
        TravelerController.class,
        UserController.class,
        WithWhomController.class
})
public class GlobalRestExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    private ResponseEntity<?> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponseDto(status.value(), message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage()); // 409 Conflict
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage()); // 404 Not Found
    }


}
