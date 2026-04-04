package com.backapi.backapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Стандартная обёртка для всех успешных ответов API.
 * Фронт ожидает: { data: T, message?: string }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseWrapper<T> {
    private T data;
    private String message;

    public static <T> ApiResponseWrapper<T> of(T data) {
        return ApiResponseWrapper.<T>builder().data(data).build();
    }

    public static <T> ApiResponseWrapper<T> of(T data, String message) {
        return ApiResponseWrapper.<T>builder().data(data).message(message).build();
    }
}
