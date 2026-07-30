package com.umrah.scanner.common.response;

/** The envelope every successful response body is wrapped in. */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
