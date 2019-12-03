package no.fint.kulturtanken.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SchoolOwnerNotFoundException extends RuntimeException {

    public SchoolOwnerNotFoundException(String message) {
        super(message);
    }
}
