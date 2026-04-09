package info.jemsit.notification_service.config.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioConfiguration(
        String accountSid,
        String authToken,
        String trialNumber,
        String specialNumber
) {
}
