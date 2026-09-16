package com.devguard.shared.error;

public class TenantResolutionException extends RuntimeException {
    public TenantResolutionException(String message) {
        super(message);
    }
}
