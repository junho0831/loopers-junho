package com.loopers.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    // 범용 에러
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 리소스입니다."),

    // User 관련 에러
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "ID는 필수 입력 항목입니다"),
    INVALID_USER_ID_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_USER_ID_LENGTH", "ID는 영문 및 숫자 10자 이내여야 합니다"),
    INVALID_USER_ID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_USER_ID_FORMAT", "ID는 영문 및 숫자만 사용 가능합니다"),

    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일은 필수 입력 항목입니다"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_FORMAT", "올바른 이메일 형식이 아닙니다"),

    INVALID_BIRTHDAY(HttpStatus.BAD_REQUEST, "INVALID_BIRTHDAY", "생년월일은 필수 입력 항목입니다"),
    INVALID_BIRTHDAY_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_BIRTHDAY_FORMAT", "생년월일은 yyyy-MM-dd 형식이어야 합니다"),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_CHARGE_AMOUNT", "충전 금액은 0보다 큰 값이어야 합니다"),
    INVALID_GENDER(HttpStatus.BAD_REQUEST, "INVALID_GENDER", "성별을 선택해주세요"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 가입된 ID입니다"),

    //내 정보
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),

    //포인트
    INVALID_POINT_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_POINT_FORMAT", "0 보다 작을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
