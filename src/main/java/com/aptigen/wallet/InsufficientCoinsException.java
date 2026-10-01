package com.aptigen.wallet;

public class InsufficientCoinsException extends RuntimeException {
    public InsufficientCoinsException(String message) {
        super(message);
    }
}