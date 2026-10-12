package com.sergiorodriguez.listacompra.web;

public record UserResponse(
        Long id,
        String email,
        String displayName
) {
}
