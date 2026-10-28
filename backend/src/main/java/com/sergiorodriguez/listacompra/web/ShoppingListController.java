package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de la lista de la compra por defecto. Solo se ocupa de HTTP; la lógica está en
 * {@link ShoppingListService}. Todas las rutas exigen JWT y todas devuelven la lista entera
 * con su total estimado, para que el cliente la pinte sin otra petición.
 */
@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    /** GET /api/shopping-list: la lista por defecto del usuario (se crea la primera vez). */
    @GetMapping
    public ShoppingListResponse obtener(@AuthenticationPrincipal String email) {
        return shoppingListService.listaPorDefecto(email);
    }

    /**
     * POST /api/shopping-list/items: añade un producto. Si ya estaba, suma los envases.
     * Responde 201 con la lista, o 404 si el producto no existe o es de otro usuario.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListResponse anadir(@AuthenticationPrincipal String email,
                                       @Valid @RequestBody ShoppingItemRequest request) {
        return shoppingListService.anadir(email, request);
    }

    /** PATCH /api/shopping-list/items/{id}: cambia los envases. 404 si no existe o es de otro. */
    @PatchMapping("/items/{id}")
    public ShoppingListResponse cambiarEnvases(@AuthenticationPrincipal String email,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ShoppingPackagesRequest request) {
        return shoppingListService.cambiarEnvases(email, id, request.packages());
    }

    /** PATCH /api/shopping-list/items/{id}/checked: marca o desmarca un item. */
    @PatchMapping("/items/{id}/checked")
    public ShoppingListResponse marcar(@AuthenticationPrincipal String email,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ShoppingCheckedRequest request) {
        return shoppingListService.marcar(email, id, request.checked());
    }

    /** DELETE /api/shopping-list/items/{id}: quita un item. Responde 200 con la lista restante. */
    @DeleteMapping("/items/{id}")
    public ShoppingListResponse borrar(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return shoppingListService.borrar(email, id);
    }
}