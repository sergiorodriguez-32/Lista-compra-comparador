import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ProductResponse } from '../../core/products';
import { ShoppingItemResponse, ShoppingListResponse } from '../../core/shopping-list';
import { ShoppingListPage } from './shopping-list';

describe('ShoppingListPage', () => {
  let fixture: ComponentFixture<ShoppingListPage>;
  let http: HttpTestingController;

  const huevos: ProductResponse = {
    id: 1, source: 'MANUAL', name: 'Huevos L', brand: 'Gallina Feliz', description: null,
    price: 2.5, packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: 'Huevos',
  };
  const itemHuevos: ShoppingItemResponse = {
    id: 7, product: huevos, packages: 2, checked: false, subtotal: 5,
  };
  // Construye una lista con los items que le pases y el total calculado
  const listaCon = (...items: ShoppingItemResponse[]): ShoppingListResponse => ({
    id: 1, name: 'Mi lista', defaultList: true, items,
    estimatedTotal: items.reduce((suma, i) => suma + i.subtotal, 0),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShoppingListPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ShoppingListPage);
    fixture.detectChanges(); // ngOnInit lanza las dos peticiones
  });

  afterEach(() => http.verify());

  const pantalla = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  async function cargar(lista: ShoppingListResponse, productos: ProductResponse[]) {
    http.expectOne('/api/shopping-list').flush(lista);
    http.expectOne('/api/products').flush(productos);
    await fixture.whenStable();
  }

  it('mientras llega la respuesta muestra "Cargando"', () => {
    http.expectOne('/api/shopping-list'); // pendiente, sin responder
    http.expectOne('/api/products');
    expect(pantalla()).toContain('Cargando lista');
  });

  it('muestra los items con sus envases y el total estimado', async () => {
    await cargar(listaCon(itemHuevos), [huevos]);

    expect(pantalla()).toContain('Huevos L');
    expect(pantalla()).toContain('Gallina Feliz');
    expect(pantalla()).toContain('2 envases');
    expect(pantalla()).toContain('Total estimado: 5.00 €');
    expect(pantalla()).not.toContain('Cargando');
  });

  it('si la lista está vacía lo dice', async () => {
    await cargar(listaCon(), [huevos]);

    expect(pantalla()).toContain('Tu lista de la compra está vacía');
  });

  it('si el usuario no tiene productos le dice que cree uno primero', async () => {
    await cargar(listaCon(), []);

    expect(pantalla()).toContain('primero crea un producto');
  });

  it('si la API falla muestra el error y permite reintentar', async () => {
    http.expectOne('/api/shopping-list').flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudo cargar la lista de la compra');
    expect(pantalla()).toContain('Reintentar');
  });

  it('al pulsar +1 hace PATCH con los envases sumados y actualiza la lista', async () => {
    await cargar(listaCon(itemHuevos), [huevos]);

    fixture.componentInstance.cambiar(itemHuevos, 1);

    const peticion = http.expectOne('/api/shopping-list/items/7');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ packages: 3 });
    peticion.flush(listaCon({ ...itemHuevos, packages: 3, subtotal: 7.5 }));
    await fixture.whenStable();

    expect(pantalla()).toContain('3 envases');
    expect(pantalla()).toContain('Total estimado: 7.50 €');
  });

  it('los envases nunca bajan de 1: restar con 1 no envía nada', async () => {
    const uno: ShoppingItemResponse = { ...itemHuevos, packages: 1, subtotal: 2.5 };
    await cargar(listaCon(uno), [huevos]);

    fixture.componentInstance.cambiar(uno, -1);

    http.expectNone('/api/shopping-list/items/7');
  });

  it('al añadir hace POST con productId y packages y muestra la lista devuelta', async () => {
    await cargar(listaCon(), [huevos]);

    fixture.componentInstance.form.setValue({ productId: 1, packages: 2 });
    fixture.componentInstance.anadir();

    const peticion = http.expectOne('/api/shopping-list/items');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ productId: 1, packages: 2 });
    peticion.flush(listaCon(itemHuevos));
    await fixture.whenStable();

    expect(pantalla()).toContain('Huevos L');
    expect(fixture.componentInstance.items().length).toBe(1);
  });

  it('con el formulario vacío no envía nada', async () => {
    await cargar(listaCon(), [huevos]);
    fixture.componentInstance.form.patchValue({ packages: null });

    fixture.componentInstance.anadir();

    http.expectNone('/api/shopping-list/items');
  });

  it('al marcar hace PATCH a /checked y el item queda marcado', async () => {
    await cargar(listaCon(itemHuevos), [huevos]);

    fixture.componentInstance.marcar(itemHuevos, true);

    const peticion = http.expectOne('/api/shopping-list/items/7/checked');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ checked: true });
    peticion.flush(listaCon({ ...itemHuevos, checked: true }));
    await fixture.whenStable();

    expect(fixture.componentInstance.items()[0].checked).toBe(true);
  });

  it('al quitar (y confirmar) hace DELETE y muestra la lista restante', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await cargar(listaCon(itemHuevos), [huevos]);

    fixture.componentInstance.borrar(itemHuevos);

    const peticion = http.expectOne('/api/shopping-list/items/7');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(listaCon()); // el servidor devuelve la lista restante, no null
    await fixture.whenStable();

    expect(pantalla()).not.toContain('Huevos L');
    expect(pantalla()).toContain('Tu lista de la compra está vacía');
  });

  it('si el usuario cancela la confirmación no quita nada', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await cargar(listaCon(itemHuevos), [huevos]);

    fixture.componentInstance.borrar(itemHuevos);

    http.expectNone('/api/shopping-list/items/7');
    expect(pantalla()).toContain('Huevos L');
  });

  it('si falla un cambio muestra el error pero conserva la lista', async () => {
    await cargar(listaCon(itemHuevos), [huevos]);

    fixture.componentInstance.cambiar(itemHuevos, 1);
    http.expectOne('/api/shopping-list/items/7')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudo cambiar "Huevos L"');
    expect(pantalla()).toContain('Huevos L');
  });
});
