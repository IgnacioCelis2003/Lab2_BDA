package com.Lab1BDA.Backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para ser lanzada cuando un recurso no se encuentra.
 * La anotación @ResponseStatus le dice a Spring que devuelva un
 * código HTTP 404 (NOT_FOUND) cuando esta excepción es lanzada.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}