package com.loopers.support.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
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
    INVALID_POINT_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_POINT_FORMAT", "0 보다 작을 수 없습니다"),
    INVALID_USE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_USE_AMOUNT", "사용 금액은 0보다 큰 값이어야 합니다"),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINTS", "포인트가 부족합니다"),

    //상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"),
    INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST, "INVALID_SORT_TYPE", "잘못된 정렬 타입입니다"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK", "상품 재고가 부족합니다"),

    //브랜드
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다"),

    //쿠폰
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다"),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "COUPON_ALREADY_USED", "이미 사용된 쿠폰입니다"),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "COUPON_NOT_OWNED", "해당 쿠폰을 소유하고 있지 않습니다"),
    INVALID_COUPON_CONDITION(HttpStatus.BAD_REQUEST, "INVALID_COUPON_CONDITION", "쿠폰 사용 조건을 만족하지 않습니다"),

    //주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorType(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
