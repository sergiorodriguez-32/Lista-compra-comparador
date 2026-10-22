package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.PackageUnit;
import com.sergiorodriguez.listacompra.core.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos para crear o editar un producto manual (RF-15). Los límites coinciden con las
 * columnas de la tabla {@code products}. No incluye owner ni source: los pone el servidor,
 * nunca el cliente (si no, alguien podría crear productos a nombre de otro usuario).
 */
public record ProductRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 100)
        String brand,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        // inclusive = false: tiene que ser MAYOR que 0 (un envase de 0 unidades no existe)
        @NotNull
        @DecimalMin(value = "0.000", inclusive = false)
        @Digits(integer = 7, fraction = 3)
        BigDecimal packageAmount,

        @NotNull
        PackageUnit packageUnit,

        @NotNull
        ProductCategory category,

        @Size(max = 5000)
        String ingredients
) {
}