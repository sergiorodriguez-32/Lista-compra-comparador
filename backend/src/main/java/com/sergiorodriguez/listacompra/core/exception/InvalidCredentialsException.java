package com.sergiorodriguez.listacompra.core.exception;

/**
 * Se lanza cuando el email no existe o la contraseña no coincide. Se usa el mismo mensaje
 * para ambos casos a propósito, para no revelar qué emails están registrados.
 */

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales incorrectas");
    }
}

