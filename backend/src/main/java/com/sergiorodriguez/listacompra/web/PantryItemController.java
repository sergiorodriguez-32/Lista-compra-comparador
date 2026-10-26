package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.PantryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de la despensa. Solo se ocupa de HTTP (rutas, JSON, códigos de respuesta); la
 * lógica está en {@link PantryItemService}. Todas las rutas exigen JWT: el usuario sale del
 * token, nunca de lo que envíe el cliente.
 */
@RestController
@RequestMapping("/api/pantry")
@RequiredArgsConstructor
public class PantryItemController {

    private final PantryItemService pantryItemService;

    /**
     * POST /api/pantry: añade un producto a la despensa. Si ya estaba, suma la cantidad.
     * Responde 201 con el item, o 404 si el producto no existe o es de otro usuario.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItemResponse anadir(@AuthenticationPrincipal String email,
                                     @Valid @RequestBody PantryItemRequest request) {
        return pantryItemService.anadir(email, request);
    }

    /** GET /api/pantry: la despensa del usuario. */
    @GetMapping
    public List<PantryItemResponse> listar(@AuthenticationPrincipal String email) {
        return pantryItemService.listar(email);
    }

    /** PATCH /api/pantry/{id}: cambia la cantidad de un item. 404 si no existe o es de otro. */
    @PatchMapping("/{id}")
    public PantryItemResponse cambiarCantidad(@AuthenticationPrincipal String email,
                                              @PathVariable Long id,
                                              @Valid @RequestBody PantryQuantityRequest request) {
        return pantryItemService.cambiarCantidad(email, id, request.quantity());
    }

    /** DELETE /api/pantry/{id}: quita un item de la despensa. Responde 204 sin cuerpo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@AuthenticationPrincipal String email, @PathVariable Long id) {
        pantryItemService.borrar(email, id);
    }

    /** GET /api/pantry/low-stock: items de la despensa en el umbral de stock bajo del usuario o por debajo. */
    @GetMapping("/low-stock")
    public List<PantryItemResponse> stockBajo(@AuthenticationPrincipal String email) {
        return pantryItemService.stockBajo(email);
    }
}