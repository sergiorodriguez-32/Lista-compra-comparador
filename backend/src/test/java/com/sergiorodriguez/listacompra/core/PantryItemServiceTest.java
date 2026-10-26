package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.PantryItemNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.web.PantryItemRequest;
import com.sergiorodriguez.listacompra.web.PantryItemResponse;
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
class PantryItemServiceTest {

    private static final String EMAIL = "ana@b.com";

    @Mock
    private PantryItemRepository pantryItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PantryItemService pantryItemService;

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

    /** Un item de la despensa de Ana con la cantidad indicada. */
    private PantryItem itemDeAna(Long id, Product producto, String cantidad) {
        PantryItem item = new PantryItem();
        item.setId(id);
        item.setOwner(ana);
        item.setProduct(producto);
        item.setQuantity(new BigDecimal(cantidad));
        return item;
    }

    @Test
    @DisplayName("anadir un producto que no estaba crea un item nuevo a nombre del usuario")
    void anadir_productoNuevo_creaItem() {
        Product huevos = productoDeAna(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(huevos));
        when(pantryItemRepository.findByOwnerIdAndProductId(7L, 1L)).thenReturn(Optional.empty());
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PantryItemResponse respuesta = pantryItemService.anadir(EMAIL,
                new PantryItemRequest(1L, new BigDecimal("6")));

        assertThat(respuesta.quantity()).isEqualByComparingTo("6");

        ArgumentCaptor<PantryItem> captor = ArgumentCaptor.forClass(PantryItem.class);
        verify(pantryItemRepository).save(captor.capture());
        PantryItem guardado = captor.getValue();
        assertThat(guardado.getOwner()).isSameAs(ana);
        assertThat(guardado.getProduct()).isSameAs(huevos);
    }

    @Test
    @DisplayName("anadir un producto que ya estaba suma la cantidad al item existente")
    void anadir_productoExistente_sumaCantidad() {
        Product huevos = productoDeAna(1L);
        PantryItem existente = itemDeAna(5L, huevos, "6");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(huevos));
        when(pantryItemRepository.findByOwnerIdAndProductId(7L, 1L)).thenReturn(Optional.of(existente));
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PantryItemResponse respuesta = pantryItemService.anadir(EMAIL,
                new PantryItemRequest(1L, new BigDecimal("6")));

        assertThat(respuesta.id()).isEqualTo(5L); // el mismo item, no uno nuevo
        assertThat(respuesta.quantity()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("anadir un producto que no es del usuario lanza ProductNotFoundException y no guarda nada")
    void anadir_productoAjeno_lanzaNoEncontradoYNoGuarda() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pantryItemService.anadir(EMAIL,
                new PantryItemRequest(99L, new BigDecimal("1"))))
                .isInstanceOf(ProductNotFoundException.class);

        verify(pantryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("listar devuelve solo los items del usuario")
    void listar_buscaPorElIdDelUsuario() {
        Product huevos = productoDeAna(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByOwnerId(7L))
                .thenReturn(List.of(itemDeAna(5L, huevos, "6"), itemDeAna(6L, huevos, "2")));

        List<PantryItemResponse> lista = pantryItemService.listar(EMAIL);

        assertThat(lista).extracting(PantryItemResponse::id).containsExactly(5L, 6L);
    }

    @Test
    @DisplayName("stockBajo consulta con el umbral propio del usuario")
    void stockBajo_usaElUmbralDelUsuario() {
        BigDecimal umbral = new BigDecimal("2");
        ana.setLowStockThreshold(umbral);
        Product huevos = productoDeAna(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByOwnerIdAndQuantityLessThanEqual(7L, umbral))
                .thenReturn(List.of(itemDeAna(6L, huevos, "2")));

        List<PantryItemResponse> lista = pantryItemService.stockBajo(EMAIL);

        assertThat(lista).extracting(PantryItemResponse::id).containsExactly(6L);
    }

    @Test
    @DisplayName("stockBajo devuelve lista vacía si ningún item llega al umbral")
    void stockBajo_sinItemsBajos_devuelveListaVacia() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByOwnerIdAndQuantityLessThanEqual(7L, ana.getLowStockThreshold()))
                .thenReturn(List.of());

        List<PantryItemResponse> lista = pantryItemService.stockBajo(EMAIL);

        assertThat(lista).isEmpty();
    }

    @Test
    @DisplayName("cambiarCantidad sustituye la cantidad del item del usuario")
    void cambiarCantidad_itemPropio_sustituyeLaCantidad() {
        PantryItem existente = itemDeAna(5L, productoDeAna(1L), "12");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByIdAndOwnerId(5L, 7L)).thenReturn(Optional.of(existente));

        PantryItemResponse respuesta = pantryItemService.cambiarCantidad(EMAIL, 5L, new BigDecimal("3"));

        assertThat(respuesta.quantity()).isEqualByComparingTo("3");
        assertThat(existente.getQuantity()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("cambiarCantidad de un item ajeno lanza PantryItemNotFoundException (RF-04)")
    void cambiarCantidad_itemAjeno_lanzaNoEncontrado() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pantryItemService.cambiarCantidad(EMAIL, 99L, new BigDecimal("3")))
                .isInstanceOf(PantryItemNotFoundException.class);
    }

    @Test
    @DisplayName("borrar elimina el item del usuario")
    void borrar_itemPropio_loElimina() {
        PantryItem existente = itemDeAna(5L, productoDeAna(1L), "6");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByIdAndOwnerId(5L, 7L)).thenReturn(Optional.of(existente));

        pantryItemService.borrar(EMAIL, 5L);

        verify(pantryItemRepository).delete(existente);
    }

    @Test
    @DisplayName("borrar un item ajeno lanza PantryItemNotFoundException y no borra nada")
    void borrar_itemAjeno_lanzaNoEncontradoYNoBorra() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(pantryItemRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pantryItemService.borrar(EMAIL, 99L))
                .isInstanceOf(PantryItemNotFoundException.class);

        verify(pantryItemRepository, never()).delete(any());
    }
}