package com.sergiorodriguez.listacompra.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una lista de la compra. Cada usuario tiene una lista por defecto (su "carrito"
 * permanente) y puede crear más con nombre. Fila de {@code shopping_lists} (Flyway V4).
 */
@Entity
@Table(name = "shopping_lists")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultList = false;

}