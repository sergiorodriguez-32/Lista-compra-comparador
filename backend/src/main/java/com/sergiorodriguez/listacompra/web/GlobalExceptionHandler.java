package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.exception.EmailAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP con un formato único (RNF-06).
 * Vigila los errores de todos los controladores. Usa {@link ProblemDetail}, la clase de Spring
 * que representa un error según el estándar RFC 7807 (status, title, detail...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Email ya registrado: la lanza UserService. Responde 409 Conflict. */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEmailDuplicado(EmailAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Datos de entrada no válidos: la lanza Spring cuando falla @Valid en un @RequestBody.
     * Responde 400 Bad Request con el detalle de qué campo falla y por qué.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        // mapa campo -> mensaje de error, p. ej. {"email": "must be a well-formed email address"}
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problema.setProperty("errores", errores); // se añade al JSON de respuesta
        return problema;
    }
}
