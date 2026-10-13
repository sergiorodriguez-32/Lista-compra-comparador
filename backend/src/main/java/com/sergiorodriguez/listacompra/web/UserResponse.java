package com.sergiorodriguez.listacompra.web;

/**
 * Datos de un usuario que devuelve la API. A propósito no incluye ni la contraseña ni su hash.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName
) {
}
