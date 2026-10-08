package vn.edu.fpt.service;

import java.util.Map;

public class ValidationException extends Exception {
    private final transient Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Dữ liệu không hợp lệ");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
