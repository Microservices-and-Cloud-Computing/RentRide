package com.rentride.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration for the Notification Microservice.
 *
 * <p>Provides a dedicated {@link ThreadPoolTaskExecutor} for non-blocking email
 * dispatch tasks. Isolating the mail thread pool prevents slow SMTP connections
 * from starving the Tomcat request threads that serve the REST API.
 *
 * <p>Thread pool parameters:
 * <ul>
 *   <li>Core pool size: 4 — always-ready threads for immediate dispatch</li>
 *   <li>Max pool size: 8 — burst capacity under high notification load</li>
 *   <li>Queue capacity: 50 — buffered tasks before new threads spawn</li>
 *   <li>Thread name prefix: {@code Notification-Mail-} — identifiable in logs/stack traces</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated task executor for asynchronous email notification dispatch.
     *
     * <p>Referenced by name {@code "notificationTaskExecutor"} in
     * {@code @Async("notificationTaskExecutor")} annotations on service methods.
     *
     * @return configured {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Notification-Mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

