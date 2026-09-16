package com.devguard.shared.error;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) { super(resource + " no encontrado: " + id); }
}
