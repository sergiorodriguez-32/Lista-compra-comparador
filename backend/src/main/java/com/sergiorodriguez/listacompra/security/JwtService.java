package com.sergiorodriguez.listacompra.security;

import com.sergiorodriguez.listacompra.core.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Crea y valida los JWT. Firma los tokens con una clave secreta que solo conoce el servidor:
 * si alguien modifica un token, la firma deja de coincidir y se rechaza.
 */
@Service
public class JwtService {

    private final SecretKey key;     // clave con la que se firma y se verifica
    private final long expirationMs; // cuánto dura un token, en milisegundos

    // @Value inyecta aquí los valores de application.properties (que vienen del .env)
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        // jjwt no acepta un String como clave: hay que convertirlo en una SecretKey
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Crea un token firmado para el usuario. Se llama justo después de comprobar su contraseña.
     */
    public String generateToken(User user) {
        Date now = new Date(); // se usa dos veces: para "emitido" y para calcular la caducidad
        return Jwts.builder()                          // empieza a construir un token vacío
                .subject(String.valueOf(user.getId())) // claim "sub": a quién pertenece (id)
                .claim("email", user.getEmail())       // claim propio: el email
                .issuedAt(now)                         // claim "iat": cuándo se emitió
                .expiration(new Date(now.getTime() + expirationMs)) // claim "exp": cuándo caduca
                .signWith(key)                         // calcula la firma con la clave secreta
                .compact();                            // une cabecera.payload.firma en un String
    }

    /**
     * Valida el token y devuelve el email que lleva dentro. No consulta la base de datos:
     * todo lo que necesita está en el propio token.
     *
     * @throws io.jsonwebtoken.JwtException si la firma no es válida, el token está
     *         manipulado o ha caducado (el filtro de seguridad lo traducirá a un 401)
     */
    public String extractEmail(String token) {
        return Jwts.parser()                  // empieza a construir un lector de tokens
                .verifyWith(key)              // comprobará la firma con NUESTRA clave
                .build()                      // termina de configurar el lector
                .parseSignedClaims(token)     // valida firma y caducidad; si falla, lanza excepción
                .getPayload()                 // el contenido ya verificado (objeto Claims)
                .get("email", String.class);  // saca el claim "email" como String
    }
}
