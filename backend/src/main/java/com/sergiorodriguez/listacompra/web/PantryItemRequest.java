package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Datos para añadir un producto a la despensa. La cantidad va en la unidad base del
 * producto (huevos, kg, litros). No incluye owner: lo pone el servidor.
 */
public record PantryItemRequest(

        @NotNull
        Long productId,

        @NotNull
        @DecimalMin("0.000")
        @Digits(integer = 7, fraction = 3)
        BigDecimal quantity
) {
}