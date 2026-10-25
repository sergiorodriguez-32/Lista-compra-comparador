package com.sergiorodriguez.listacompra.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Un producto que el usuario tiene en casa. Cada objeto es una fila de la tabla
 * {@code pantry_items} (migración Flyway V3). La cantidad va en la unidad base del
 * producto (huevos, kg, litros), no en envases.
 */
@Entity
@Table(name = "pantry_items")
@Getter
@Setter
@NoArgsConstructor
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Muchos items de despensa pueden ser de un mismo usuario.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Muchos items pueden apuntar al mismo producto (de usuarios distintos).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

}