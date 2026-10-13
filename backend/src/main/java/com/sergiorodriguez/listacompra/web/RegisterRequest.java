package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía el cliente para registrarse (cuerpo JSON de POST /api/auth/register).
 * Las validaciones solo se ejecutan si el controlador recibe el objeto con {@code @Valid}.
 * Los límites de longitud coinciden con las columnas de la tabla {@code users}.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        // Contraseña en claro: solo existe aquí, en memoria; se cifra antes de guardarla
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 100)
        String displayName
) {
}
