package com.todo.exception;

import java.sql.DataTruncation;

public class DatabaseException extends RuntimeException{

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

}
