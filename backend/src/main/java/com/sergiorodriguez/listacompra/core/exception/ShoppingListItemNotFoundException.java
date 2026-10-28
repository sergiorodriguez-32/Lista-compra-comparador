package com.sergiorodriguez.listacompra.core.exception;

/**
 * Se lanza cuando un item de la lista de la compra no existe o no pertenece al usuario.
 * Mismo error en los dos casos para no revelar que el id existe.
 */
public class ShoppingListItemNotFoundException extends RuntimeException {

    public ShoppingListItemNotFoundException(Long id) {
        super("Item de la lista de la compra no encontrado: " + id);
    }
}
