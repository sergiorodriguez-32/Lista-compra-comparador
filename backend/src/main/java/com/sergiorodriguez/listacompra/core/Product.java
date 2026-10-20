package com.sergiorodriguez.listacompra.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Producto del catálogo. Cada objeto es una fila de la tabla {@code products}
 * (creada por la migración Flyway V2). Los productos manuales tienen {@code owner};
 * los de tienda (más adelante) lo tendrán a null y serán compartidos.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // STRING guarda el nombre ("SECO"). ORDINAL guardaría la posición (0, 1...) y romper el
    // orden del enum corrompería los datos, así que no se usa nunca.
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ProductSource source = ProductSource.MANUAL;

    @Column(name = "external_id", length = 100)
    private String externalId;

    // Muchos productos pueden ser de un mismo usuario. LAZY: no carga el User entero hasta
    // que se use. Es nullable porque los productos de tienda no tienen dueño.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "package_amount", nullable = false, precision = 10, scale = 3)
    private BigDecimal packageAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_unit", nullable = false, length = 10)
    private PackageUnit packageUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ProductCategory category;

    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String ingredients;

}