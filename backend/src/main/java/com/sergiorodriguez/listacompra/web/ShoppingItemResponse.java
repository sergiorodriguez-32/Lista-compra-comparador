package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.ShoppingListItem;

import java.math.BigDecimal;

/** Un item de la lista tal como lo devuelve la API, con el subtotal ya calculado. */
public record ShoppingItemResponse(
        Long id,
        ProductResponse product,
        int packages,
        boolean checked,
        BigDecimal subtotal
) {

    public static ShoppingItemResponse from(ShoppingListItem item) {
        BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getPackages()));
        return new ShoppingItemResponse(item.getId(), ProductResponse.from(item.getProduct()),
                item.getPackages(), item.isChecked(), subtotal);
    }
}