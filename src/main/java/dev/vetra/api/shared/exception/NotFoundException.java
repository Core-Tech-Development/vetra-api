package dev.vetra.api.shared.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String entityName, Object id) {
        super("NOT_FOUND", entityName + " not found with id: " + id);
    }

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
