package com.sergiorodriguez.listacompra.web;

import jakarta.validation.constraints.NotNull;

/** Marca o desmarca un item de la lista. Es solo visual: no mueve nada a la despensa. */
public record ShoppingCheckedRequest(

        @NotNull
        Boolean checked
) {
}