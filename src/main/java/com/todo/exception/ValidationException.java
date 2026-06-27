package com.todo.exception;

import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Validation Failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return this.errors;
    }
}
