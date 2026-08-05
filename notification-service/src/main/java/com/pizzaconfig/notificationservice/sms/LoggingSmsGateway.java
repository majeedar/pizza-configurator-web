package com.pizzaconfig.notificationservice.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Placeholder gateway: logs instead of sending a real SMS. Swap for a Twilio/AWS SNS/Vonage
// client once real credentials exist — everything else in this service is provider-agnostic.
@Component
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("SMS to {}: {}", phoneNumber, message);
    }
}
