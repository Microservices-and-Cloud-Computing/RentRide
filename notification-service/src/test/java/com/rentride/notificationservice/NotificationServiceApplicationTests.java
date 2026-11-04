package com.rentride.notificationservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context load verification test for NotificationServiceApplication.
 *
 * <p>Verifies that the Spring ApplicationContext bootstraps cleanly,
 * including {@code JavaMailSender}, {@code TemplateEngine} (Thymeleaf),
 * and all configuration beans.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Test
    @DisplayName("Context Loads: Verifies complete Notification Microservice Spring context bootstraps cleanly")
    void contextLoads() {
        // Passes if the ApplicationContext initialises without errors.
        // Implicitly verifies: JavaMailSender bean, Thymeleaf TemplateEngine bean,
        // MailProperties config binding, and Tomcat servlet container startup.
    }
}
