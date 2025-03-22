/*
 * ErrorResponseDto.java
 * REST API 예외 응답용 DTO (에러 상태 코드와 메시지 전달)
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성
 * ========================================================
 */
package nadeuli.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private int statusCode;
    private String message;
}