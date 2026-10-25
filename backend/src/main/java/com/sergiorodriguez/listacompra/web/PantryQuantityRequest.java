package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Nueva cantidad de un item de la despensa. Reemplaza a la anterior, no la suma. */
public record PantryQuantityRequest(

        @NotNull
        @DecimalMin("0.000")
        @Digits(integer = 7, fraction = 3)
        BigDecimal quantity
) {
}