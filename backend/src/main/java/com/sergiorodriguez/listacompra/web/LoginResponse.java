package com.sergiorodriguez.listacompra.web;

/**
 * Respuesta del login: el JWT que el cliente enviará en cada petición posterior.
 */

public record LoginResponse(

        String token
) {
}
