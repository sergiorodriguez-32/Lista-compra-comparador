import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProductRequest, ProductResponse, Products } from './products';

describe('Products', () => {
  let service: Products;
  let http: HttpTestingController; // servidor falso: intercepta las peticiones y responde lo que le digamos

  const datos: ProductRequest = {
    name: 'Huevos L', brand: 'Gallina Feliz', description: null, price: 2.5,
    packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: null,
  };
  const respuesta: ProductResponse = { id: 1, source: 'MANUAL', ...datos };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Products);
    http = TestBed.inject(HttpTestingController);
  });

  // Falla el test si se envió alguna petición que el test no esperaba
  afterEach(() => http.verify());

  it('list hace GET /api/products y devuelve la lista', () => {
    let recibido: ProductResponse[] | undefined;
    service.list().subscribe(lista => (recibido = lista));

    const peticion = http.expectOne('/api/products');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([respuesta]); // responde con esta lista

    expect(recibido).toEqual([respuesta]);
  });

  it('create hace POST /api/products con los datos en el cuerpo', () => {
    let recibido: ProductResponse | undefined;
    service.create(datos).subscribe(p => (recibido = p));

    const peticion = http.expectOne('/api/products');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush(respuesta);

    expect(recibido?.id).toBe(1);
  });

  it('update hace PUT /api/products/{id} con los datos en el cuerpo', () => {
    service.update(1, datos).subscribe();

    const peticion = http.expectOne('/api/products/1');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush(respuesta);
  });

  it('delete hace DELETE /api/products/{id}', () => {
    service.delete(1).subscribe();

    const peticion = http.expectOne('/api/products/1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
  });
});
