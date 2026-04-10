package info.jemsit.notification_service.service.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import info.jemsit.notification_service.config.sms.twilio.TwilioConfiguration;
import info.jemsit.notification_service.service.SmsRequestDTO;
import info.jemsit.notification_service.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("twilio")
@Slf4j
@RequiredArgsConstructor
public class TwilioSmsSender implements SmsService {

    private  final TwilioConfiguration twilioConfiguration;


    @Override
    public void sendSms(SmsRequestDTO request, String otp) {
        try {
            PhoneNumber to = new PhoneNumber(request.phoneNumber());
            PhoneNumber from = new PhoneNumber(twilioConfiguration.trialNumber());
            String message = " Your OTP is: " + otp;
            MessageCreator creator = Message.creator(to, from, message);
            creator.create();
            log.info("SMS sent to {}", request.phoneNumber());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}. Error: {}", request.phoneNumber(), e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage());
        }
    }
}
