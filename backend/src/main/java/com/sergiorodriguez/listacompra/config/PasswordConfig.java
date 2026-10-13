package com.sergiorodriguez.listacompra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Define el codificador de contraseñas de la aplicación.
 */
@Configuration // Spring lee esta clase al arrancar para registrar los beans que define
public class PasswordConfig {

    /**
     * Bean inyectable en cualquier clase que pida un {@link PasswordEncoder}. Se devuelve la
     * interfaz para que el resto del código no dependa del algoritmo concreto (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
