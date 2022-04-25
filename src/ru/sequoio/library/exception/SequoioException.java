package ru.sequoio.library.exception;

public class SequoioException extends RuntimeException{

    public SequoioException(String message) {
        super(message);
    }

    public SequoioException(String message, Throwable cause) {
        super(message, cause);
    }
}
