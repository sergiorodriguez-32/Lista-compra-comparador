package com.sergiorodriguez.listacompra.core.exception;

/**
 * Se lanza cuando un item de la despensa no existe o no pertenece al usuario. Igual que con
 * los productos, es el mismo error en los dos casos para no revelar que el id existe.
 */
public class PantryItemNotFoundException extends RuntimeException {

    public PantryItemNotFoundException(Long id) {
        super("Item de despensa no encontrado: " + id);
    }
}