package com.hanmo.flowplan.global.error;

import com.hanmo.flowplan.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 1. 비즈니스 로직 에러 (우리가 직접 throw한 것들)
   */
  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    log.error("BusinessException: {}", e.getMessage());
    return ErrorResponse.toResponseEntity(e.getErrorCode());
  }

  /**
   * 2. @Valid 검증 실패 (RequestBody 필드 에러)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.error("Validation Failed: {}", e.getMessage());
    return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE);
  }

  /**
   * 3. JSON 파싱 에러 (그 줄바꿈 문자 에러, 날짜 포맷 에러 등)
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
    log.error("JSON Parse Error: {}", e.getMessage());

    // 날짜 포맷 문제인 경우 상세 처리
    if (e.getCause() instanceof DateTimeParseException) {
      return ErrorResponse.toResponseEntity(ErrorCode.INVALID_DATE_FORMAT);
    }

    return ErrorResponse.toResponseEntity(ErrorCode.INVALID_JSON_FORMAT);
  }

  /**
   * 4. 권한 없음 (403) - SecurityConfig에서 넘어온 것들
   */
  @ExceptionHandler(AccessDeniedException.class)
  protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
    log.error("Access Denied: {}", e.getMessage());
    return ErrorResponse.toResponseEntity(ErrorCode.ACCESS_DENIED);
  }

  /**
   * 5. 나머지 모든 예외 (500)
   */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Internal Server Error: {}", e.getMessage(), e); // 스택트레이스 출력
    return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
  }
}