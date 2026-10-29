import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ShoppingList, ShoppingListResponse } from './shopping-list';

describe('ShoppingList', () => {
  let service: ShoppingList;
  let http: HttpTestingController;

  const respuesta: ShoppingListResponse = {
    id: 1, name: 'Mi lista', defaultList: true, estimatedTotal: 5,
    items: [{
      id: 7, packages: 2, checked: false, subtotal: 5,
      product: {
        id: 1, source: 'MANUAL', name: 'Huevos L', brand: null, description: null, price: 2.5,
        packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: null,
      },
    }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ShoppingList);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('get hace GET /api/shopping-list y devuelve la lista', () => {
    let recibido: ShoppingListResponse | undefined;
    service.get().subscribe(l => (recibido = l));

    const peticion = http.expectOne('/api/shopping-list');
    expect(peticion.request.method).toBe('GET');
    peticion.flush(respuesta);

    expect(recibido).toEqual(respuesta);
  });

  it('add hace POST /api/shopping-list/items con productId y packages', () => {
    service.add({ productId: 1, packages: 2 }).subscribe();

    const peticion = http.expectOne('/api/shopping-list/items');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ productId: 1, packages: 2 });
    peticion.flush(respuesta);
  });

  // TODO tuyo: updatePackages → PATCH /api/shopping-list/items/7, body { packages: 3 }
  // TODO tuyo: setChecked    → PATCH /api/shopping-list/items/7/checked, body { checked: true }
  // TODO tuyo: delete        → DELETE /api/shopping-list/items/7, y comprueba que devuelve la lista (flush(respuesta))
});
