package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.PantryItem;

import java.math.BigDecimal;

/** Un item de la despensa tal como lo devuelve la API, con su producto dentro. */
public record PantryItemResponse(
        Long id,
        ProductResponse product,
        BigDecimal quantity
) {

    public static PantryItemResponse from(PantryItem item) {
        return new PantryItemResponse(item.getId(), ProductResponse.from(item.getProduct()),
                item.getQuantity());
    }
}