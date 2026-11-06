package com.rentride.vehicleservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic container for paginated query results returned to client applications.
 *
 * @param <T> element type contained within the paginated response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Generic paginated response wrapper")
public class PaginatedResponse<T> {

    @Schema(description = "List of items contained in the current page")
    private List<T> content;

    @Schema(description = "Current page index (zero-based)", example = "0")
    private int pageNo;

    @Schema(description = "Configured page size", example = "10")
    private int pageSize;

    @Schema(description = "Total number of elements matching query criteria across all pages", example = "25")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "3")
    private int totalPages;

    @Schema(description = "Whether this is the final page", example = "false")
    private boolean last;

    /**
     * Builds a {@link PaginatedResponse} from a Spring Data {@link Page}.
     *
     * @param page Spring Data page object
     * @param <T>  contained element type
     * @return populated PaginatedResponse instance
     */
    public static <T> PaginatedResponse<T> fromPage(Page<T> page) {
        if (page == null) {
            return null;
        }
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Builds a {@link PaginatedResponse} from a Spring Data {@link Page} using a mapping function.
     *
     * @param page   Spring Data source page object
     * @param mapper entity to DTO conversion function
     * @param <S>    source entity type
     * @param <T>    target response type
     * @return populated PaginatedResponse instance with mapped elements
     */
    public static <S, T> PaginatedResponse<T> fromPage(Page<S> page, Function<S, T> mapper) {
        if (page == null) {
            return null;
        }
        List<T> mappedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PaginatedResponse.<T>builder()
                .content(mappedContent)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
