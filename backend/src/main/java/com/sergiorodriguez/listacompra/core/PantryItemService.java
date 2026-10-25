package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.core.exception.PantryItemNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.web.PantryItemRequest;
import com.sergiorodriguez.listacompra.web.PantryItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lógica de negocio de la despensa. Como en los productos, cada usuario solo ve y modifica
 * lo suyo (RF-04): acceder al item de otro da el mismo error que si no existiera.
 *
 * <p>No sabe nada de HTTP. Recibe el email del usuario autenticado (el que viene en el JWT).
 */
@Service
@RequiredArgsConstructor
public class PantryItemService {

    private final PantryItemRepository pantryItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Añade un producto a la despensa. Si el usuario ya lo tenía, suma la cantidad al item
     * existente en vez de crear otro (la tabla solo permite uno por usuario y producto).
     *
     * @throws ProductNotFoundException si el producto no existe o no es del usuario
     */
    @Transactional
    public PantryItemResponse anadir(String email, PantryItemRequest request) {
        User usuario = usuario(email);
        Product producto = productRepository.findByIdAndOwnerId(request.productId(), usuario.getId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        PantryItem item = pantryItemRepository
                .findByOwnerIdAndProductId(usuario.getId(), producto.getId())
                .orElseGet(() -> {
                    PantryItem nuevo = new PantryItem();
                    nuevo.setOwner(usuario);
                    nuevo.setProduct(producto);
                    return nuevo;
                });

        item.setQuantity(item.getQuantity().add(request.quantity()));
        return PantryItemResponse.from(pantryItemRepository.save(item));
    }

    /** Lista la despensa del usuario, y solo la suya. */
    @Transactional(readOnly = true)
    public List<PantryItemResponse> listar(String email) {
        return pantryItemRepository.findByOwnerId(usuario(email).getId()).stream()
                .map(PantryItemResponse::from)
                .toList();
    }

    /** Sustituye la cantidad de un item del usuario (sumar o restar lo calcula el cliente). */
    @Transactional
    public PantryItemResponse cambiarCantidad(String email, Long id, BigDecimal cantidad) {
        PantryItem item = buscarPropio(email, id);
        item.setQuantity(cantidad);
        return PantryItemResponse.from(item);
    }

    /** Quita un item de la despensa del usuario. */
    @Transactional
    public void borrar(String email, Long id) {
        pantryItemRepository.delete(buscarPropio(email, id));
    }

    private User usuario(String email) {
        return userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    }

    /** Busca un item por id solo entre los del usuario: es la pieza clave de RF-04. */
    private PantryItem buscarPropio(String email, Long id) {
        return pantryItemRepository.findByIdAndOwnerId(id, usuario(email).getId())
                .orElseThrow(() -> new PantryItemNotFoundException(id));
    }
}