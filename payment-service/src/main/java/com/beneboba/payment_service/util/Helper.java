package com.beneboba.payment_service.util;

import java.util.Random;

public class Helper {
    public static String classifyPaymentMethod(String paymentMethod){
        return switch (paymentMethod.toUpperCase()) {
            case "BCA", "BRI", "BNI" -> "BANK";
            case "GOPAY", "OVO", "DANA" -> "EMONEY";
            default -> "UNKNOWN";
        };
    }

    public static String generateReferenceNumber() {
        Random random = new Random();
        int number = random.nextInt(90000000) + 10000000;
        return String.valueOf(number);
    }
}