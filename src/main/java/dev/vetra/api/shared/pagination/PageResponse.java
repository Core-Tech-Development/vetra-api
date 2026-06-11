package dev.vetra.api.shared.pagination;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {

    public static <T> PageResponse<T> of(List<T> content, long totalElements, PageRequest pageRequest) {
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
        return new PageResponse<>(content, totalElements, totalPages, pageRequest.page(), pageRequest.size());
    }
}
