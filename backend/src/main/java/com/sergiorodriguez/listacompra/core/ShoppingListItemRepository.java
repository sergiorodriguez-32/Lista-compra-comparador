package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    List<ShoppingListItem> findByListId(Long listId);

    Optional<ShoppingListItem> findByListIdAndProductId(Long listId, Long productId);
}