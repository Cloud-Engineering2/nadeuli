/* GlobalViewExceptionHandler.java
 * nadeuli Service - 여행
 * View 기반 전역 예외 처리 클래스 (특정 Controller 대상)
 * 작성자 : 이홍비
 * 최초 작성 일자 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : GlobalExceptionHandler
 * 이홍비    2025.03.03     AmazonS3Exception 및 UnsupportedEncodingException 처리 추가
 * 이홍비    2025.03.06     예외 발생 시 error 페이지로 이동 처리 추가
 * 박한철    2025.03.19     GlobalViewExceptionHandler로 클래스명 변경 및 Controller 명시
 * 이홍비    2025.03.20     JournalController, TravelBottomLineController 추가
 * ========================================================
 */

package nadeuli.common.exception;


import com.amazonaws.services.s3.model.AmazonS3Exception;
import jakarta.servlet.http.HttpServletResponse;
import nadeuli.controller.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.io.UnsupportedEncodingException;
import java.util.NoSuchElementException;

@ControllerAdvice(assignableTypes = {
        ShareController.class,
        ExpenseItemController.class,
        LoginController.class,
        ItineraryController.class,
        AdminController.class,
        JournalController.class,
        TravelBottomLineController.class
})
public class GlobalViewExceptionHandler {

    // 찾을 수 없을 때
    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView handleNoSuchElementException(NoSuchElementException e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus()); // 상태 코드 추가

        return modelAndView;

//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 자료를 찾을 수 없습니다. : " + e.getMessage());

    }

    // 일반적인 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus());  // 상태 코드 추가

        return modelAndView;

//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예기치 못한 오류가 발생했습니다 : " + e.getMessage());
    }

    // 예상치 못한 상황일 때
    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleIllegalStateException(IllegalStateException e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus());  // 상태 코드 추가

        return modelAndView;

//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 잘못된 요청 - 400 코드 반환
    }

    // 로그인 시 아이디 or 비밀번호 틀렸을 때
    @ExceptionHandler(BadCredentialsException.class)
    public ModelAndView handleBadCredentialsException(BadCredentialsException e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus());  // 상태 코드 추가

        return modelAndView;

//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage()); // 인증 실패 - 401 코드 반환
    }

    // AmazonS3 관련 예외 처리
    @ExceptionHandler(AmazonS3Exception.class)
    public ModelAndView handleAmazonS3Exception(AmazonS3Exception e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus());  // 상태 코드 추가

        return modelAndView;

//        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("S3 오류 발생: " + e.getMessage());
    }


    // 인코딩 관련 예외 처리
    @ExceptionHandler(UnsupportedEncodingException.class)
    public ModelAndView handleIOException(UnsupportedEncodingException e, HttpServletResponse response) {
        e.printStackTrace(); // 출력

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ModelAndView modelAndView = new ModelAndView("error"); // error.html 로 이동
        modelAndView.addObject("message", e.getMessage()); // 오류 메시지 추가
        modelAndView.addObject("statusCode", response.getStatus());  // 상태 코드 추가

        return modelAndView;
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    // api request 여부 확인
//    private boolean isApiRequest(HttpServletRequest request) {
//        return request.getRequestURI().startsWith("/api/");
//    }
}
