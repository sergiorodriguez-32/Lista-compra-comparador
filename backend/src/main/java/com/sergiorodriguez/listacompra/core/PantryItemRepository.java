package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    /** Despensa de un usuario (nunca se devuelve nada de otro usuario). */
    List<PantryItem> findByOwnerId(Long ownerId);

    /** Un item solo si es de ese usuario; si es de otro, devuelve vacío como si no existiera. */
    Optional<PantryItem> findByIdAndOwnerId(Long id, Long ownerId);

    /** Para saber si el usuario ya tiene ese producto en la despensa antes de crear otro. */
    Optional<PantryItem> findByOwnerIdAndProductId(Long ownerId, Long productId);

    /** Items del usuario cuya cantidad está en el umbral o por debajo (stock bajo). */
    List<PantryItem> findByOwnerIdAndQuantityLessThanEqual(Long ownerId, BigDecimal threshold);
}