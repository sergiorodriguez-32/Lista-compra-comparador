package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Nuevo número de envases de un item de la lista. Reemplaza al anterior, no lo suma. */
public record ShoppingPackagesRequest(

        @Min(1)
        @Max(999)
        int packages
) {
}