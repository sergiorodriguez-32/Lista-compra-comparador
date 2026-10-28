package com.sergiorodriguez.listacompra.web;

import java.math.BigDecimal;
import java.util.List;

/** Una lista de la compra con sus items y el total estimado (precio del envase × envases). */
public record ShoppingListResponse(
        Long id,
        String name,
        boolean defaultList,
        List<ShoppingItemResponse> items,
        BigDecimal estimatedTotal
) {
}