package com.sergiorodriguez.listacompra.web;

import com.sergiorodriguez.listacompra.core.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de los productos manuales. Solo se ocupa de HTTP (rutas, JSON, códigos de
 * respuesta); la lógica está en {@link ProductService}. Todas las rutas exigen JWT: el
 * usuario sale del token, nunca de lo que envíe el cliente.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * POST /api/products: crea un producto manual del usuario (RF-15). Responde 201 con el
     * producto creado o 400 si los datos no son válidos.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse crear(@AuthenticationPrincipal String email,
                                 @Valid @RequestBody ProductRequest request) {
        // @AuthenticationPrincipal: el email que JwtAuthFilter dejó en el contexto de seguridad
        return productService.crear(email, request);
    }

    /** GET /api/products: los productos del usuario. */
    @GetMapping
    public List<ProductResponse> listar(@AuthenticationPrincipal String email) {
        return productService.listar(email);
    }

    /** GET /api/products/{id}: un producto del usuario. 404 si no existe o es de otro. */
    @GetMapping("/{id}")
    public ProductResponse obtener(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return productService.obtener(email, id);
    }

    /** PUT /api/products/{id}: reemplaza los datos de un producto del usuario. */
    @PutMapping("/{id}")
    public ProductResponse actualizar(@AuthenticationPrincipal String email,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ProductRequest request) {
        return productService.actualizar(email, id, request);
    }

    /** DELETE /api/products/{id}: borra un producto del usuario. Responde 204 sin cuerpo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@AuthenticationPrincipal String email, @PathVariable Long id) {
        productService.borrar(email, id);
    }
}
