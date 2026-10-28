package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Datos para añadir un producto a la lista de la compra. Los envases son enteros. */
public record ShoppingItemRequest(

        @NotNull
        Long productId,

        @Min(1)
        @Max(999)
        int packages
) {
}