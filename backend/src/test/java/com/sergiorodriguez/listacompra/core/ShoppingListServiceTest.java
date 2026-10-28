package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.ProductNotFoundException;
import com.sergiorodriguez.listacompra.core.exception.ShoppingListItemNotFoundException;
import com.sergiorodriguez.listacompra.web.ShoppingItemRequest;
import com.sergiorodriguez.listacompra.web.ShoppingListResponse;
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
class ShoppingListServiceTest {

    private static final String EMAIL = "ana@b.com";

    @Mock
    private ShoppingListRepository listRepository;

    @Mock
    private ShoppingListItemRepository itemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShoppingListService shoppingListService;

    private User ana;

    @BeforeEach
    void preparaUsuario() {
        ana = new User();
        ana.setId(7L);
        ana.setEmail(EMAIL);
    }

    /** Un producto manual de Ana con el precio indicado, con los datos mínimos para devolverlo. */
    private Product productoDeAna(Long id, String precio) {
        Product p = new Product();
        p.setId(id);
        p.setOwner(ana);
        p.setName("Producto " + id);
        p.setPrice(new BigDecimal(precio));
        p.setPackageAmount(new BigDecimal("12"));
        p.setPackageUnit(PackageUnit.UNIDAD);
        p.setCategory(ProductCategory.FRESCO);
        return p;
    }

    /** La lista por defecto de un usuario. */
    private ShoppingList listaDe(Long id, User dueno) {
        ShoppingList lista = new ShoppingList();
        lista.setId(id);
        lista.setOwner(dueno);
        lista.setName("Mi lista");
        lista.setDefaultList(true);
        return lista;
    }

    /** Un item de una lista con los envases indicados. */
    private ShoppingListItem itemDe(Long id, ShoppingList lista, Product producto, int envases) {
        ShoppingListItem item = new ShoppingListItem();
        item.setId(id);
        item.setList(lista);
        item.setProduct(producto);
        item.setPackages(envases);
        return item;
    }

    @Test
    @DisplayName("listaPorDefecto crea la lista a nombre del usuario si no tenía ninguna")
    void listaPorDefecto_sinLista_laCrea() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(listRepository.findByOwnerIdAndDefaultListTrue(7L)).thenReturn(Optional.empty());
        when(listRepository.save(any(ShoppingList.class))).thenAnswer(inv -> {
            ShoppingList guardada = inv.getArgument(0);
            guardada.setId(10L);
            return guardada;
        });

        ShoppingListResponse respuesta = shoppingListService.listaPorDefecto(EMAIL);

        assertThat(respuesta.defaultList()).isTrue();
        assertThat(respuesta.items()).isEmpty();
        assertThat(respuesta.estimatedTotal()).isEqualByComparingTo("0");

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(listRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isSameAs(ana);
        assertThat(captor.getValue().isDefaultList()).isTrue();
    }

    @Test
    @DisplayName("listaPorDefecto con lista existente no crea otra")
    void listaPorDefecto_conLista_noCreaOtra() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(listRepository.findByOwnerIdAndDefaultListTrue(7L)).thenReturn(Optional.of(listaDe(10L, ana)));

        ShoppingListResponse respuesta = shoppingListService.listaPorDefecto(EMAIL);

        assertThat(respuesta.id()).isEqualTo(10L);
        verify(listRepository, never()).save(any());
    }

    @Test
    @DisplayName("listaPorDefecto calcula el total sumando precio del envase por número de envases")
    void listaPorDefecto_calculaElTotalEstimado() {
        ShoppingList lista = listaDe(10L, ana);
        ShoppingListItem huevos = itemDe(1L, lista, productoDeAna(1L, "2.50"), 2); // 5.00
        ShoppingListItem leche = itemDe(2L, lista, productoDeAna(2L, "1.20"), 3);  // 3.60
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(listRepository.findByOwnerIdAndDefaultListTrue(7L)).thenReturn(Optional.of(lista));
        when(itemRepository.findByListId(10L)).thenReturn(List.of(huevos, leche));

        ShoppingListResponse respuesta = shoppingListService.listaPorDefecto(EMAIL);

        assertThat(respuesta.items()).hasSize(2);
        assertThat(respuesta.estimatedTotal()).isEqualByComparingTo("8.60");
    }

    @Test
    @DisplayName("anadir un producto que no estaba crea un item nuevo en la lista del usuario")
    void anadir_productoNuevo_creaItem() {
        ShoppingList lista = listaDe(10L, ana);
        Product huevos = productoDeAna(1L, "2.50");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(huevos));
        when(listRepository.findByOwnerIdAndDefaultListTrue(7L)).thenReturn(Optional.of(lista));
        when(itemRepository.findByListIdAndProductId(10L, 1L)).thenReturn(Optional.empty());

        shoppingListService.anadir(EMAIL, new ShoppingItemRequest(1L, 2));

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(itemRepository).save(captor.capture());
        ShoppingListItem guardado = captor.getValue();
        assertThat(guardado.getList()).isSameAs(lista);
        assertThat(guardado.getProduct()).isSameAs(huevos);
        assertThat(guardado.getPackages()).isEqualTo(2);
    }

    @Test
    @DisplayName("anadir un producto que ya estaba suma los envases al item existente")
    void anadir_productoExistente_sumaEnvases() {
        ShoppingList lista = listaDe(10L, ana);
        Product huevos = productoDeAna(1L, "2.50");
        ShoppingListItem existente = itemDe(5L, lista, huevos, 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(huevos));
        when(listRepository.findByOwnerIdAndDefaultListTrue(7L)).thenReturn(Optional.of(lista));
        when(itemRepository.findByListIdAndProductId(10L, 1L)).thenReturn(Optional.of(existente));
        when(itemRepository.findByListId(10L)).thenReturn(List.of(existente));

        ShoppingListResponse respuesta = shoppingListService.anadir(EMAIL, new ShoppingItemRequest(1L, 3));

        assertThat(existente.getPackages()).isEqualTo(5); // el mismo item, no uno nuevo
        assertThat(respuesta.items()).hasSize(1);
        assertThat(respuesta.estimatedTotal()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("anadir un producto que no es del usuario lanza ProductNotFoundException y no guarda nada")
    void anadir_productoAjeno_lanzaNoEncontradoYNoGuarda() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(productRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.anadir(EMAIL, new ShoppingItemRequest(99L, 1)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEnvases sustituye los envases del item del usuario")
    void cambiarEnvases_itemPropio_sustituyeLosEnvases() {
        ShoppingList lista = listaDe(10L, ana);
        ShoppingListItem existente = itemDe(5L, lista, productoDeAna(1L, "2.50"), 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(itemRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(itemRepository.findByListId(10L)).thenReturn(List.of(existente));

        ShoppingListResponse respuesta = shoppingListService.cambiarEnvases(EMAIL, 5L, 4);

        assertThat(existente.getPackages()).isEqualTo(4);
        assertThat(respuesta.estimatedTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("cambiarEnvases de un item de la lista de otro usuario lanza ShoppingListItemNotFoundException (RF-04)")
    void cambiarEnvases_itemAjeno_lanzaNoEncontradoYNoModifica() {
        User otra = new User();
        otra.setId(8L);
        ShoppingListItem deOtra = itemDe(99L, listaDe(20L, otra), productoDeAna(1L, "2.50"), 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(itemRepository.findById(99L)).thenReturn(Optional.of(deOtra));

        assertThatThrownBy(() -> shoppingListService.cambiarEnvases(EMAIL, 99L, 5))
                .isInstanceOf(ShoppingListItemNotFoundException.class);

        assertThat(deOtra.getPackages()).isEqualTo(2); // no se tocó
    }

    @Test
    @DisplayName("marcar cambia solo la casilla visual del item del usuario")
    void marcar_itemPropio_cambiaLaCasilla() {
        ShoppingList lista = listaDe(10L, ana);
        ShoppingListItem existente = itemDe(5L, lista, productoDeAna(1L, "2.50"), 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(itemRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(itemRepository.findByListId(10L)).thenReturn(List.of(existente));

        ShoppingListResponse respuesta = shoppingListService.marcar(EMAIL, 5L, true);

        assertThat(existente.isChecked()).isTrue();
        assertThat(respuesta.items().get(0).checked()).isTrue();
        assertThat(existente.getPackages()).isEqualTo(2);
    }

    @Test
    @DisplayName("borrar elimina el item del usuario")
    void borrar_itemPropio_loElimina() {
        ShoppingList lista = listaDe(10L, ana);
        ShoppingListItem existente = itemDe(5L, lista, productoDeAna(1L, "2.50"), 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(itemRepository.findById(5L)).thenReturn(Optional.of(existente));

        shoppingListService.borrar(EMAIL, 5L);

        verify(itemRepository).delete(existente);
    }

    @Test
    @DisplayName("borrar un item ajeno lanza ShoppingListItemNotFoundException y no borra nada")
    void borrar_itemAjeno_lanzaNoEncontradoYNoBorra() {
        User otra = new User();
        otra.setId(8L);
        ShoppingListItem deOtra = itemDe(99L, listaDe(20L, otra), productoDeAna(1L, "2.50"), 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(itemRepository.findById(99L)).thenReturn(Optional.of(deOtra));

        assertThatThrownBy(() -> shoppingListService.borrar(EMAIL, 99L))
                .isInstanceOf(ShoppingListItemNotFoundException.class);

        verify(itemRepository, never()).delete(any());
    }
}