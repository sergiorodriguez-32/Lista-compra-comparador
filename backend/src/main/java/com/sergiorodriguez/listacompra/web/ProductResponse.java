package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.PackageUnit;
import com.sergiorodriguez.listacompra.core.Product;
import com.sergiorodriguez.listacompra.core.ProductCategory;
import com.sergiorodriguez.listacompra.core.ProductSource;

import java.math.BigDecimal;

/** Datos de un producto que devuelve la API. No expone al dueño. */
public record ProductResponse(
        Long id,
        ProductSource source,
        String name,
        String brand,
        String description,
        BigDecimal price,
        BigDecimal packageAmount,
        PackageUnit packageUnit,
        ProductCategory category,
        String ingredients
) {

    /** Convierte la entidad en DTO. Se usa en todos los endpoints que devuelven productos. */
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSource(), p.getName(), p.getBrand(),
                p.getDescription(), p.getPrice(), p.getPackageAmount(), p.getPackageUnit(),
                p.getCategory(), p.getIngredients());
    }
}