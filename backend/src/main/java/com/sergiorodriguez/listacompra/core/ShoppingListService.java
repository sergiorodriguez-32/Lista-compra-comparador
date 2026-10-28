package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ShoppingListItemNotFoundException;
import com.sergiorodriguez.listacompra.web.ShoppingItemRequest;
import com.sergiorodriguez.listacompra.web.ShoppingItemResponse;
import com.sergiorodriguez.listacompra.web.ShoppingListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lógica de negocio de la lista de la compra. Cada usuario tiene una lista por defecto
 * que se crea la primera vez que se pide. Como en la despensa, solo se ve y modifica lo
 * propio: acceder al item de otro da el mismo error que si no existiera.
 *
 * <p>Las cantidades van en envases a comprar, no en unidad base.
 */
@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Devuelve la lista por defecto del usuario con su total, creándola si no existe. */
    @Transactional
    public ShoppingListResponse listaPorDefecto(String email) {
        return aRespuesta(listaPorDefectoDe(usuario(email)));
    }

    /**
     * Añade un producto a la lista por defecto. Si ya estaba, suma los envases en vez de
     * crear otro item (la tabla solo permite uno por lista y producto).
     *
     * @throws ProductNotFoundException si el producto no existe o no es del usuario
     */
    @Transactional
    public ShoppingListResponse anadir(String email, ShoppingItemRequest request) {
        User usuario = usuario(email);
        Product producto = productRepository.findByIdAndOwnerId(request.productId(), usuario.getId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        ShoppingList lista = listaPorDefectoDe(usuario);

        ShoppingListItem item = itemRepository
                .findByListIdAndProductId(lista.getId(), producto.getId())
                .orElseGet(() -> {
                    ShoppingListItem nuevo = new ShoppingListItem();
                    nuevo.setList(lista);
                    nuevo.setProduct(producto);
                    nuevo.setPackages(0);
                    return nuevo;
                });

        item.setPackages(item.getPackages() + request.packages());
        itemRepository.save(item);
        return aRespuesta(lista);
    }

    /** Sustituye los envases de un item del usuario. */
    @Transactional
    public ShoppingListResponse cambiarEnvases(String email, Long itemId, int envases) {
        ShoppingListItem item = buscarPropio(email, itemId);
        item.setPackages(envases);
        return aRespuesta(item.getList());
    }

    /** Marca o desmarca un item. Es solo visual: no mueve nada a la despensa. */
    @Transactional
    public ShoppingListResponse marcar(String email, Long itemId, boolean marcado) {
        ShoppingListItem item = buscarPropio(email, itemId);
        item.setChecked(marcado);
        return aRespuesta(item.getList());
    }

    /** Quita un item de la lista del usuario. */
    @Transactional
    public ShoppingListResponse borrar(String email, Long itemId) {
        ShoppingListItem item = buscarPropio(email, itemId);
        ShoppingList lista = item.getList();
        itemRepository.delete(item);
        return aRespuesta(lista);
    }

    private User usuario(String email) {
        return userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    }

    private ShoppingList listaPorDefectoDe(User usuario) {
        return listRepository.findByOwnerIdAndDefaultListTrue(usuario.getId())
                .orElseGet(() -> {
                    ShoppingList nueva = new ShoppingList();
                    nueva.setOwner(usuario);
                    nueva.setName("Mi lista");
                    nueva.setDefaultList(true);
                    return listRepository.save(nueva);
                });
    }

    /** Busca un item solo entre los de listas del usuario: es la pieza clave de RF-04. */
    private ShoppingListItem buscarPropio(String email, Long itemId) {
        Long ownerId = usuario(email).getId();
        return itemRepository.findById(itemId)
                .filter(item -> item.getList().getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new ShoppingListItemNotFoundException(itemId));
    }

    private ShoppingListResponse aRespuesta(ShoppingList lista) {
        List<ShoppingItemResponse> items = itemRepository.findByListId(lista.getId()).stream()
                .map(ShoppingItemResponse::from)
                .toList();
        BigDecimal total = items.stream()
                .map(ShoppingItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ShoppingListResponse(lista.getId(), lista.getName(), lista.isDefaultList(), items, total);
    }
}