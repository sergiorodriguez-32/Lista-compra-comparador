package com.sergiorodriguez.listacompra.core.exception;

/**
 * Se lanza cuando un producto no existe o no pertenece al usuario. A propósito es el mismo
 * error en los dos casos: así no se revela a un usuario que el id de otro existe.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Producto no encontrado: " + id);
    }
}