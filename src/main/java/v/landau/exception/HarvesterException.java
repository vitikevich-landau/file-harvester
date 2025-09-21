package v.landau.exception;

/**
 * Custom exception for file harvesting operations.
 */
public class HarvesterException extends Exception {

    public HarvesterException(String message) {
        super(message);
    }

    public HarvesterException(String message, Throwable cause) {
        super(message, cause);
    }
}