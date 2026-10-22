package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.web.ProductRequest;
import com.sergiorodriguez.listacompra.web.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negocio de los productos manuales (RF-15). Cada usuario solo ve y modifica los
 * suyos (RF-04): todas las consultas pasan por el dueño, así que acceder a un producto de
 * otro usuario da el mismo error que si no existiera (RNF-03).
 *
 * <p>No sabe nada de HTTP. Recibe el email del usuario autenticado (el que viene en el JWT).
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Crea un producto manual. El dueño y el origen los pone el servidor, nunca el cliente. */
    @Transactional
    public ProductResponse crear(String email, ProductRequest request) {
        Product producto = new Product();
        producto.setSource(ProductSource.MANUAL);
        producto.setOwner(usuario(email));
        copiarDatos(producto, request);
        return ProductResponse.from(productRepository.save(producto));
    }

    /** Lista los productos del usuario, y solo los suyos. */
    @Transactional(readOnly = true)
    public List<ProductResponse> listar(String email) {
        return productRepository.findByOwnerId(usuario(email).getId()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * Devuelve un producto del usuario.
     *
     * @throws ProductNotFoundException si no existe o es de otro usuario (se traduce a un 404)
     */
    @Transactional(readOnly = true)
    public ProductResponse obtener(String email, Long id) {
        return ProductResponse.from(buscarPropio(email, id));
    }

    /** Sustituye los datos de un producto del usuario por los recibidos. */
    @Transactional
    public ProductResponse actualizar(String email, Long id, ProductRequest request) {
        Product producto = buscarPropio(email, id);
        copiarDatos(producto, request);
        // No hace falta save(): la entidad está gestionada por JPA dentro de la transacción
        // y Hibernate guarda los cambios solo al terminar el método.
        return ProductResponse.from(producto);
    }

    /** Borra un producto del usuario. */
    @Transactional
    public void borrar(String email, Long id) {
        productRepository.delete(buscarPropio(email, id));
    }

    /**
     * El usuario del token. Si el token es válido pero el usuario ya no existe (se borró su
     * cuenta), se trata como credenciales inválidas.
     */
    private User usuario(String email) {
        return userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    }

    /** Busca un producto por id solo entre los del usuario: es la pieza clave de RF-04. */
    private Product buscarPropio(String email, Long id) {
        return productRepository.findByIdAndOwnerId(id, usuario(email).getId())
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Pasa los datos del DTO a la entidad (un único sitio, para crear y para actualizar). */
    private void copiarDatos(Product producto, ProductRequest request) {
        producto.setName(request.name());
        producto.setBrand(request.brand());
        producto.setDescription(request.description());
        producto.setPrice(request.price());
        producto.setPackageAmount(request.packageAmount());
        producto.setPackageUnit(request.packageUnit());
        producto.setCategory(request.category());
        producto.setIngredients(request.ingredients());
    }
}
