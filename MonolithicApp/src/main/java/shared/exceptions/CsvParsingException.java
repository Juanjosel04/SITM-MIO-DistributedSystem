package shared.exceptions;

public class CsvParsingException extends Exception {
    public CsvParsingException(String message) {
        super(message);
    }

    public CsvParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
