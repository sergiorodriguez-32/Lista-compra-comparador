package com.sergiorodriguez.listacompra.core;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Usuario de la aplicación. Cada objeto es una fila de la tabla {@code users}
 * (creada por la migración Flyway V1).
 * <p>
 * Con {@code ddl-auto=validate}, Hibernate comprueba al arrancar que esta clase encaja con
 * la tabla; la estructura real la define Flyway, no estas anotaciones.
 */
@Entity
@Table(name = "users") // "user" es palabra reservada en PostgreSQL, por eso la tabla va en plural
@Getter
@Setter
@NoArgsConstructor // JPA necesita un constructor sin argumentos para crear los objetos
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // el id lo genera la BD (columna IDENTITY)
    private Long id; // Long (objeto) y no long: vale null hasta que se guarda

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Hash BCrypt de la contraseña. Nunca se guarda ni se devuelve la contraseña en claro. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Umbral global de stock bajo (RF-03). Valor por defecto 1, igual que en la BD. */
    @Column(name = "low_stock_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal lowStockThreshold = BigDecimal.ONE;

}
