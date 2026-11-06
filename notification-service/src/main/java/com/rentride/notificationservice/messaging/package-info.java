/**
 * AMQP message listener components for the Notification Microservice.
 *
 * <p>Contains {@link com.rentride.notificationservice.messaging.NotificationEventListener}
 * which consumes domain events from RabbitMQ queues and delegates to the email
 * notification pipeline.
 */
package com.rentride.notificationservice.messaging;

