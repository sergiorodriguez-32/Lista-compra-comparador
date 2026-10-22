package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.web.ProductRequest;
import com.sergiorodriguez.listacompra.web.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String EMAIL = "ana@b.com";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private User ana;

    @BeforeEach
    void preparaUsuario() {
        ana = new User();
        ana.setId(7L);
        ana.setEmail(EMAIL);
    }

    /** Un producto manual de Ana, con los datos mínimos para devolverlo. */
    private Product productoDeAna(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setOwner(ana);
        p.setName("Huevos L");
        p.setPrice(new BigDecimal("2.50"));
        p.setPackageAmount(new BigDecimal("12"));
        p.setPackageUnit(PackageUnit.UNIDAD);
        p.setCategory(ProductCategory.FRESCO);
        return p;
    }

    private ProductRequest request(String nombre) {
        return new ProductRequest(nombre, "Gallina Feliz", null, new BigDecimal("2.90"),
                new BigDecimal("12"), PackageUnit.UNIDAD, ProductCategory.FRESCO, "Huevos");
    }

    @Test
    @DisplayName("crear guarda el producto como MANUAL y a nombre del usuario del token")
    void crear_guardaProductoManualConElDuenoDelToken() {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L); // simula el id que asigna la BD
            return p;
        });

        // WHEN
        ProductResponse respuesta = productService.crear(EMAIL, request("Huevos M"));

        // THEN: respuesta
        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.name()).isEqualTo("Huevos M");
        assertThat(respuesta.source()).isEqualTo(ProductSource.MANUAL);

        // THEN: lo que se guardó
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product guardado = captor.getValue();
        assertThat(guardado.getOwner()).isSameAs(ana);
        assertThat(guardado.getSource()).isEqualTo(ProductSource.MANUAL);
        assertThat(guardado.getPackageAmount()).isEqualByComparingTo("12");
        assertThat(guardado.getCategory()).isEqualTo(ProductCategory.FRESCO);
    }

    @Test
    @DisplayName("crear con un usuario que ya no existe lanza InvalidCredentialsException y no guarda nada")
    void crear_conUsuarioInexistente_lanzaExcepcionYNoGuarda() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.crear(EMAIL, request("Huevos M")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("listar devuelve solo los productos del usuario")
    void listar_buscaPorElIdDelUsuario() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByOwnerId(7L)).thenReturn(List.of(productoDeAna(1L), productoDeAna(2L)));

        List<ProductResponse> lista = productService.listar(EMAIL);

        assertThat(lista).extracting(ProductResponse::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("obtener devuelve el producto si es del usuario")
    void obtener_productoPropio_loDevuelve() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(productoDeAna(1L)));

        ProductResponse respuesta = productService.obtener(EMAIL, 1L);

        assertThat(respuesta.name()).isEqualTo("Huevos L");
    }

    @Test
    @DisplayName("obtener un producto que no es del usuario lanza ProductNotFoundException (RF-04)")
    void obtener_productoAjeno_lanzaNoEncontrado() {
        // El repositorio busca por id Y dueño: si el id es de otro usuario, devuelve vacío
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.obtener(EMAIL, 99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("actualizar cambia los datos del producto del usuario")
    void actualizar_productoPropio_cambiaLosDatos() {
        Product existente = productoDeAna(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(existente));

        ProductResponse respuesta = productService.actualizar(EMAIL, 1L, request("Huevos XL"));

        assertThat(respuesta.name()).isEqualTo("Huevos XL");
        assertThat(existente.getName()).isEqualTo("Huevos XL");
        assertThat(existente.getPrice()).isEqualByComparingTo("2.90");
        assertThat(existente.getOwner()).isSameAs(ana); // el dueño nunca cambia
    }

    @Test
    @DisplayName("actualizar un producto ajeno lanza ProductNotFoundException")
    void actualizar_productoAjeno_lanzaNoEncontrado() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.actualizar(EMAIL, 99L, request("Huevos XL")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("borrar elimina el producto del usuario")
    void borrar_productoPropio_loElimina() {
        Product existente = productoDeAna(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(existente));

        productService.borrar(EMAIL, 1L);

        verify(productRepository).delete(existente);
    }

    @Test
    @DisplayName("borrar un producto ajeno lanza ProductNotFoundException y no borra nada")
    void borrar_productoAjeno_lanzaNoEncontradoYNoBorra() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.borrar(EMAIL, 99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any());
    }
}
