package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.exception.EmailAlreadyRegisteredException;
import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.core.exception.PantryItemNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ShoppingListItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    /**
     * JSON mal formado o con un valor que no existe (p. ej. una categoría "PIZZA"): la lanza
     * Spring al leer el cuerpo, antes de llegar a @Valid. Sin este método, Spring la reenvía a
     * la ruta interna /error, que está protegida, y el cliente recibiría un 401 engañoso.
     * Responde 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleJsonNoLegible(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o contiene un valor no permitido");
    }

    /** Email o contraseña incorrectos: la lanza UserService en el login. Responde 401. */
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleCredencialesInvalidas(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Producto inexistente o de otro usuario: la lanza ProductService. Responde 404. */
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProductoNoEncontrado(ProductNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PantryItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleItemDespensaNoEncontrado(PantryItemNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Item de la lista de la compra inexistente o de otro usuario. Responde 404. */
    @ExceptionHandler(ShoppingListItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleItemCompraNoEncontrado(ShoppingListItemNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
