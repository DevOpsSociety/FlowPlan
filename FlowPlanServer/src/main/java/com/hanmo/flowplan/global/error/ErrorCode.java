package com.hanmo.flowplan.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  /* 400 BAD_REQUEST : 잘못된 요청 */
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
  INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "JSON 형식이 잘못되었습니다. (줄바꿈 문자 등 확인 필요)"), // ⭐️ 그 JSON 에러
  INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (YYYY-MM-DD)"), // ⭐️ 그 날짜 에러
  MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),

  /* 401 UNAUTHORIZED : 인증되지 않은 사용자 */
  UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."), // ⭐️ JWT 관련

  /* 403 FORBIDDEN : 권한 없음 */
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."), // ⭐️ 403 에러
  NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "해당 프로젝트의 멤버가 아닙니다."), // ⭐️ ProjectMemberValidator

  /* 404 NOT_FOUND : Resource 를 찾을 수 없음 */
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 프로젝트를 찾을 수 없습니다."),
  TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 작업을 찾을 수 없습니다."),

  /* 409 CONFLICT : Resource 의 현재 상태와 충돌. 보통 중복 발생 시 사용 */
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "데이터가 이미 존재합니다."),

  /* 500 INTERNAL_SERVER_ERROR : 서버 내부 오류 */
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
  AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서버와의 통신 중 오류가 발생했습니다."); // ⭐️ AI 422/500 에러

  private final HttpStatus httpStatus;
  private final String message;
}