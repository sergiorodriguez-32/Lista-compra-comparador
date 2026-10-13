package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de autenticación. Solo se ocupa de HTTP (rutas, JSON, códigos de respuesta);
 * la lógica está en {@link UserService}.
 */
@RestController // atiende peticiones HTTP y devuelve JSON
@RequestMapping("/api/auth") // prefijo de todas las rutas de esta clase
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * POST /api/auth/register: crea un usuario nuevo (RF-01). Responde 201 con el usuario
     * creado (sin contraseña), 400 si los datos no son válidos o 409 si el email ya existe.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201 en vez del 200 por defecto
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        // @RequestBody convierte el JSON en el record; @Valid ejecuta las validaciones del record
        return userService.register(request);
    }

}
