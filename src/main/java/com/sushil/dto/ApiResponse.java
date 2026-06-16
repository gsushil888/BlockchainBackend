package com.sushil.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    boolean       success;
    int           statusCode;
    String        message;
    T             data;
    ApiError      error;
    String        path;
    LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message, HttpStatus status, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ApiError error, HttpStatus status, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(status.value())
                .error(error)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        String code;
        String message;
        Object details;

        public static ApiError of(String code, String message) {
            return ApiError.builder().code(code).message(message).build();
        }

        public static ApiError of(String code, String message, Object details) {
            return ApiError.builder().code(code).message(message).details(details).build();
        }
    }
}
