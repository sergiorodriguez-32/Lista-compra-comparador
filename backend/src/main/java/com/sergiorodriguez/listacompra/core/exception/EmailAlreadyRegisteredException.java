package com.sergiorodriguez.listacompra.core.exception;

/**
 * Se lanza al registrar un usuario cuyo email ya existe. Es una RuntimeException (no hay que
 * declararla con {@code throws}); un manejador global la convertirá en una respuesta 409.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("El email ya está registrado: " + email);
    }
}
