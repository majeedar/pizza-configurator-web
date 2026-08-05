package com.pizzaconfig.notificationservice.sms;

public interface SmsGateway {
    void send(String phoneNumber, String message);
}
