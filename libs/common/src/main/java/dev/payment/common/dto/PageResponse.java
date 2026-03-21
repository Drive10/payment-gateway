package dev.payment.common.dto;

import dev.payment.common.api.Meta;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        Meta meta
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new Meta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }
}
