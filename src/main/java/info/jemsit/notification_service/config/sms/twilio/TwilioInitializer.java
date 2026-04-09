package info.jemsit.notification_service.config.sms.twilio;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TwilioInitializer {
    private final TwilioConfiguration twilioConfiguration;

    @PostConstruct
    public void init(){
        Twilio.init(twilioConfiguration.accountSid(), twilioConfiguration.authToken());
        log.info("Twilio initialized with account SID: {}", twilioConfiguration.accountSid());
        log.info("Twilio trial number: {}", twilioConfiguration.trialNumber());
    }
}
