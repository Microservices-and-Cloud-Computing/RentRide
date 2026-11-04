package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative Spring Cloud OpenFeign client for RPC communication with user-service.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url:}",
        configuration = FeignClientConfig.class,
        fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Retrieves user profile details by ID.
     *
     * @param id user ID
     * @return UserResponseDto with profile info
     */
    @GetMapping("/api/v1/users/{id}")
    UserResponseDto getUserById(@PathVariable("id") Long id);
}
