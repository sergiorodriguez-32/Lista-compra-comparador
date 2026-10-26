import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Pantry, PantryItemResponse } from './pantry';

describe('Pantry', () => {
  let service: Pantry;
  let http: HttpTestingController; // servidor falso: intercepta las peticiones y responde lo que le digamos

  const respuesta: PantryItemResponse = {
    id: 5,
    quantity: 6,
    product: {
      id: 1, source: 'MANUAL', name: 'Huevos L', brand: null, description: null, price: 2.5,
      packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: null,
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Pantry);
    http = TestBed.inject(HttpTestingController);
  });

  // Falla el test si se envió alguna petición que el test no esperaba
  afterEach(() => http.verify());

  it('list hace GET /api/pantry y devuelve la lista', () => {
    let recibido: PantryItemResponse[] | undefined;
    service.list().subscribe(lista => (recibido = lista));

    const peticion = http.expectOne('/api/pantry');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([respuesta]);

    expect(recibido).toEqual([respuesta]);
  });

  it('lowStock hace GET /api/pantry/low-stock y devuelve los items', () => {
    let recibido: PantryItemResponse[] | undefined;
    service.lowStock().subscribe(lista => (recibido = lista));

    const peticion = http.expectOne('/api/pantry/low-stock');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([respuesta]);

    expect(recibido).toEqual([respuesta]);
  });

  it('add hace POST /api/pantry con productId y quantity en el cuerpo', () => {
    let recibido: PantryItemResponse | undefined;
    service.add({ productId: 1, quantity: 6 }).subscribe(i => (recibido = i));

    const peticion = http.expectOne('/api/pantry');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ productId: 1, quantity: 6 });
    peticion.flush(respuesta);

    expect(recibido?.id).toBe(5);
  });

  it('updateQuantity hace PATCH /api/pantry/{id} solo con la cantidad', () => {
    service.updateQuantity(5, 3).subscribe();

    const peticion = http.expectOne('/api/pantry/5');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ quantity: 3 });
    peticion.flush({ ...respuesta, quantity: 3 });
  });

  it('delete hace DELETE /api/pantry/{id}', () => {
    service.delete(5).subscribe();

    const peticion = http.expectOne('/api/pantry/5');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
  });
});
