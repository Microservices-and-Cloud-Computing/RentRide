package com.rentride.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response wrapper for admin user queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated list of user records")
public class PaginatedUserResponse {

    @Schema(description = "List of user profile records for current page")
    private List<UserResponse> content;

    @Schema(description = "Zero-based current page number", example = "0")
    private int pageNo;

    @Schema(description = "Requested page size limit", example = "10")
    private int pageSize;

    @Schema(description = "Total number of user records matching criteria", example = "25")
    private long totalElements;

    @Schema(description = "Total number of pages available", example = "3")
    private int totalPages;

    @Schema(description = "Flag indicating if this is the final page", example = "false")
    private boolean last;

    /**
     * Factory method creating a {@link PaginatedUserResponse} from a Spring Data {@link Page}.
     *
     * @param page Spring Data page of {@link UserResponse}
     * @return populated {@link PaginatedUserResponse}
     */
    public static PaginatedUserResponse fromPage(Page<UserResponse> page) {
        if (page == null) {
            return null;
        }
        return PaginatedUserResponse.builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
