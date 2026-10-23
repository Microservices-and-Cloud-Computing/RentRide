package com.rentride.userservice.dto;

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
public class PaginatedUserResponse {

    private List<UserResponse> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
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
