package com.employmentvc.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service
 * 
 * Responsibilities:
 * - Asynchronous notification delivery (email, SMS, push, webhooks)
 * - Notification templating and personalization
 * - Delivery status tracking and retries
 * - User notification preferences management
 * - Integration with external notification providers
 * - Event-driven notification triggers
 * 
 * Channels:
 * - Email (SMTP, SendGrid, AWS SES)
 * - SMS (Twilio, AWS SNS)
 * - Push Notifications (FCM, APNs)
 * - Webhooks (custom integrations)
 * 
 * Architecture: Event-driven, queue-based processing
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
