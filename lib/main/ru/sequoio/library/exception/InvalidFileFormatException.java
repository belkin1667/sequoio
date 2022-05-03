package ru.sequoio.library.exception;

public class InvalidFileFormatException extends SequoioException {

    public InvalidFileFormatException(String path, String message) {
        super(String.format("Invalid format of file %s: %s", path, message));
    }

    public InvalidFileFormatException(String path, String message, Throwable cause) {
        super(String.format("Invalid format of file %s: %s", path, message), cause);
    }

}
