package com.sergiorodriguez.listacompra.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que se ejecuta una vez en cada petición, antes de llegar al controlador. Lee el JWT
 * de la cabecera {@code Authorization: Bearer <token>}; si es válido, marca al usuario como
 * autenticado. Si no hay token o no es válido, no autentica nada y Spring Security responderá
 * 401 en las rutas protegidas.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Solo se intenta autenticar si hay cabecera con formato "Bearer <token>"
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()); // quita "Bearer "
            try {
                // Valida firma y caducidad; si algo falla lanza JwtException
                String email = jwtService.extractEmail(token);

                // Se guarda "quién es" en el contexto de seguridad de esta petición.
                // El principal es el email; sin contraseña (null) y sin roles (lista vacía).
                var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Token manipulado, caducado o mal formado: se ignora y la petición
                // sigue sin autenticar, así que las rutas protegidas darán 401.
                SecurityContextHolder.clearContext();
            }
        }

        // Pase lo que pase, la petición continúa hacia el siguiente filtro / el controlador
        filterChain.doFilter(request, response);
    }
}
