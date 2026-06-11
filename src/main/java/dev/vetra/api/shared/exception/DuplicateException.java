package dev.vetra.api.shared.exception;

public class DuplicateException extends BusinessException {

    public DuplicateException(String entityName, String field, Object value) {
        super("DUPLICATE", entityName + " already exists with " + field + ": " + value);
    }

    public DuplicateException(String message) {
        super("DUPLICATE", message);
    }
}
