package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos que envía el cliente para iniciar sesión (cuerpo JSON de POST /api/auth/login).
 */

public record LoginRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {


}
