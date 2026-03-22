package com.quickserv.quickserv.entity;

/**
 * Enum for supported payment methods
 */
public enum PaymentMethod {
    CARD("Credit/Debit Card"),
    UPI("UPI"),
    WALLET("Digital Wallet"),
    CASH("Cash on Service");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

