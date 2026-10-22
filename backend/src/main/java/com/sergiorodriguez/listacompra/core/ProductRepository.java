package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Productos de un usuario (RF-04: nunca se devuelve nada de otro usuario). */
    List<Product> findByOwnerId(Long ownerId);

    /**
     * Un producto solo si es de ese usuario. Es la pieza clave de la seguridad: si el id
     * existe pero es de otro, devuelve vacío, igual que si no existiera.
     */
    Optional<Product> findByIdAndOwnerId(Long id, Long ownerId);
}