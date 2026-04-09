package info.jemsit.notification_service.service;

public interface SmsService {
        void sendSms(SmsRequestDTO request, String otp);
}
