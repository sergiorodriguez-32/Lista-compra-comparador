package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    List<ShoppingList> findByOwnerId(Long ownerId);

    Optional<ShoppingList> findByOwnerIdAndDefaultListTrue(Long ownerId);

    Optional<ShoppingList> findByIdAndOwnerId(Long id, Long ownerId);
}