package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.UserResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for {@link UserServiceClient} when user-service is unavailable.
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserResponseDto getUserById(Long id) {
        log.warn("[FALLBACK] UserServiceClient fallback triggered for userId={}", id);
        return null;
    }
}
