package no.fint.kulturtanken.Exceptions;

public class ResourceRequestTimeoutException extends RuntimeException {
    public ResourceRequestTimeoutException(String message) {
        super(message);
    }
}
