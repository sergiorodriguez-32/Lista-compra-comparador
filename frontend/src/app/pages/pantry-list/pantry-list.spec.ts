import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { PantryItemResponse } from '../../core/pantry';
import { ProductResponse } from '../../core/products';
import { PantryList } from './pantry-list';

describe('PantryList', () => {
  let fixture: ComponentFixture<PantryList>;
  let http: HttpTestingController;

  const huevos: ProductResponse = {
    id: 1, source: 'MANUAL', name: 'Huevos L', brand: 'Gallina Feliz', description: null,
    price: 2.5, packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: 'Huevos',
  };
  const itemHuevos: PantryItemResponse = { id: 5, product: huevos, quantity: 6 };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PantryList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PantryList);
    fixture.detectChanges(); // primer ciclo: ngOnInit lanza las dos peticiones (despensa y productos)
  });

  afterEach(() => http.verify());

  /** Texto que ve el usuario en pantalla */
  const pantalla = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  /** Responde a las tres peticiones que lanza ngOnInit (el stock bajo es opcional: por defecto, ninguno). */
  async function cargar(
    items: PantryItemResponse[],
    productos: ProductResponse[],
    bajos: PantryItemResponse[] = [],
  ) {
    http.expectOne('/api/pantry').flush(items);
    http.expectOne('/api/products').flush(productos);
    http.expectOne('/api/pantry/low-stock').flush(bajos);
    await fixture.whenStable();
  }

  it('mientras llega la respuesta muestra "Cargando"', () => {
    http.expectOne('/api/pantry'); // pendiente, sin responder
    http.expectOne('/api/products');
    http.expectOne('/api/pantry/low-stock');
    expect(pantalla()).toContain('Cargando despensa');
  });

  it('muestra los items de la despensa con su cantidad', async () => {
    await cargar([itemHuevos], [huevos]);

    expect(pantalla()).toContain('Huevos L');
    expect(pantalla()).toContain('Gallina Feliz');
    expect(pantalla()).toContain('6 unidades');
    expect(pantalla()).not.toContain('Cargando');
  });

  it('si la despensa está vacía lo dice', async () => {
    await cargar([], [huevos]);

    expect(pantalla()).toContain('Tu despensa está vacía');
  });

  it('si el usuario no tiene productos le dice que cree uno primero', async () => {
    await cargar([], []);

    expect(pantalla()).toContain('primero crea un producto');
  });

  it('si la API falla muestra el error y permite reintentar', async () => {
    http.expectOne('/api/pantry').flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/products').flush([huevos]);
    http.expectOne('/api/pantry/low-stock').flush([]);
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudo cargar la despensa');
    expect(pantalla()).toContain('Reintentar');
  });

  it('al pulsar +1 hace PATCH con la cantidad sumada y actualiza la lista', async () => {
    await cargar([itemHuevos], [huevos]);

    fixture.componentInstance.cambiar(itemHuevos, 1);

    const peticion = http.expectOne('/api/pantry/5');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ quantity: 7 });
    peticion.flush({ ...itemHuevos, quantity: 7 });
    http.expectOne('/api/pantry/low-stock').flush([]); // al cambiar la cantidad se vuelve a pedir
    await fixture.whenStable();

    expect(pantalla()).toContain('7 unidades');
  });

  it('la cantidad nunca baja de 0: restar con 0 no envía nada', async () => {
    const vacio: PantryItemResponse = { ...itemHuevos, quantity: 0 };
    await cargar([vacio], [huevos]);

    fixture.componentInstance.cambiar(vacio, -1);

    http.expectNone('/api/pantry/5');
  });

  it('al añadir un producto nuevo hace POST y lo muestra en la lista', async () => {
    await cargar([], [huevos]);

    fixture.componentInstance.form.setValue({ productId: 1, quantity: 6 });
    fixture.componentInstance.anadir();

    const peticion = http.expectOne('/api/pantry');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ productId: 1, quantity: 6 });
    peticion.flush(itemHuevos);
    http.expectOne('/api/pantry/low-stock').flush([]);
    await fixture.whenStable();

    expect(pantalla()).toContain('Huevos L');
    expect(fixture.componentInstance.lista().length).toBe(1);
  });

  it('si el producto ya estaba, el item se actualiza en vez de duplicarse', async () => {
    await cargar([itemHuevos], [huevos]);

    fixture.componentInstance.form.setValue({ productId: 1, quantity: 6 });
    fixture.componentInstance.anadir();
    http.expectOne('/api/pantry').flush({ ...itemHuevos, quantity: 12 }); // el servidor suma
    http.expectOne('/api/pantry/low-stock').flush([]);
    await fixture.whenStable();

    expect(fixture.componentInstance.lista().length).toBe(1);
    expect(pantalla()).toContain('12 unidades');
  });

  it('con el formulario vacío no envía nada', async () => {
    await cargar([], [huevos]);

    fixture.componentInstance.anadir();

    http.expectNone('/api/pantry');
  });

  it('al quitar (y confirmar) hace DELETE y saca el item de la lista', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await cargar([itemHuevos], [huevos]);

    fixture.componentInstance.borrar(itemHuevos);

    const peticion = http.expectOne('/api/pantry/5');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
    http.expectOne('/api/pantry/low-stock').flush([]);
    await fixture.whenStable();

    expect(pantalla()).not.toContain('Huevos L');
    expect(pantalla()).toContain('Tu despensa está vacía');
  });

  it('si hay productos en stock bajo muestra el aviso con sus nombres', async () => {
    await cargar([itemHuevos], [huevos], [itemHuevos]);

    expect(pantalla()).toContain('Te queda poco de: Huevos L');
  });

  it('si no hay stock bajo no muestra el aviso', async () => {
    await cargar([itemHuevos], [huevos], []);

    expect(pantalla()).not.toContain('Te queda poco');
  });

  it('al restar vuelve a pedir el stock bajo y el aviso aparece', async () => {
    await cargar([itemHuevos], [huevos], []);

    fixture.componentInstance.cambiar(itemHuevos, -1);
    http.expectOne('/api/pantry/5').flush({ ...itemHuevos, quantity: 5 });
    http.expectOne('/api/pantry/low-stock').flush([{ ...itemHuevos, quantity: 5 }]);
    await fixture.whenStable();

    expect(pantalla()).toContain('Te queda poco de: Huevos L');
  });

  it('si falla la petición del stock bajo no hay aviso pero la lista sigue', async () => {
    http.expectOne('/api/pantry').flush([itemHuevos]);
    http.expectOne('/api/products').flush([huevos]);
    http.expectOne('/api/pantry/low-stock').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(pantalla()).toContain('Huevos L');
    expect(pantalla()).not.toContain('Te queda poco');
  });

  it('si el usuario cancela la confirmación no quita nada', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await cargar([itemHuevos], [huevos]);

    fixture.componentInstance.borrar(itemHuevos);

    http.expectNone('/api/pantry/5');
    expect(pantalla()).toContain('Huevos L');
  });
});
