package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Acceso a datos de {@link User}. Es solo una interfaz: Spring Data genera la implementación
 * al arrancar. {@code JpaRepository<User, Long>} ya incluye save, findById, findAll, delete...
 * (el segundo tipo es el tipo del @Id).
 */
public interface UserRepository extends JpaRepository<User, Long> {

   /** Busca por email (lo usará el login). Optional porque puede no existir. */
   Optional<User> findByEmail(String email);

   /** Comprueba si un email ya está registrado (lo usa el registro). */
   boolean existsByEmail(String email);

}
