package com.backapi.backapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Пагинированный ответ, совпадающий с фронтовым PaginatedResponse<T>.
 * Фронт ожидает: { content, totalElements, totalPages, page, size }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    /**
     * Конвертирует Spring Page в PaginatedResponse.
     */
    public static <T> PaginatedResponse<T> from(Page<T> springPage) {
        return PaginatedResponse.<T>builder()
                .content(springPage.getContent())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .build();
    }
}
