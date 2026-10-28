package com.sergiorodriguez.listacompra.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un producto dentro de una lista de la compra. La cantidad va en envases a comprar
 * (2 packs de 12 huevos = 2), no en unidad base. Fila de {@code shopping_list_items}.
 */
@Entity
@Table(name = "shopping_list_items")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList list;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "packages", nullable = false)
    private int packages = 1;

    @Column(name = "checked", nullable = false)
    private boolean checked = false;

}