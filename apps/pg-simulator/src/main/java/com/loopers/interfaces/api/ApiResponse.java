package com.loopers.interfaces.api;

public class ApiResponse<T> {
    private final Metadata meta;
    private final T data;

    public ApiResponse(Metadata meta, T data) {
        this.meta = meta;
        this.data = data;
    }

    public Metadata getMeta() { return meta; }
    public T getData() { return data; }

    public static class Metadata {
        public enum Result { SUCCESS, FAIL }

        private final Result result;
        private final String errorCode;
        private final String message;

        public Metadata(Result result, String errorCode, String message) {
            this.result = result;
            this.errorCode = errorCode;
            this.message = message;
        }

        public Result getResult() { return result; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }

        public static Metadata success() {
            return new Metadata(Result.SUCCESS, null, null);
        }

        public static Metadata fail(String errorCode, String message) {
            return new Metadata(Result.FAIL, errorCode, message);
        }
    }

    public static ApiResponse<Object> success() {
        return new ApiResponse<>(Metadata.success(), null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Metadata.success(), data);
    }

    public static ApiResponse<Object> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(Metadata.fail(errorCode, errorMessage), null);
    }
}

