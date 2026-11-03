import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ProductResponse } from '../../core/products';
import { ProductList } from './product-list';

describe('ProductList', () => {
  let fixture: ComponentFixture<ProductList>;
  let http: HttpTestingController;

  const huevos: ProductResponse = {
    id: 1, source: 'MANUAL', name: 'Huevos L', brand: 'Gallina Feliz', description: null,
    price: 2.5, packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: 'Huevos',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProductList);
    fixture.detectChanges(); // primer ciclo: se ejecuta ngOnInit y sale la petición
  });

  afterEach(() => http.verify());

  /** Texto que ve el usuario en pantalla */
  const pantalla = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('mientras llega la respuesta muestra "Cargando"', () => {
    http.expectOne('/api/products'); // la petición está pendiente, sin responder
    expect(pantalla()).toContain('Cargando');
  });

  it('muestra los productos que devuelve la API', async () => {
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    expect(pantalla()).toContain('Huevos L');
    expect(pantalla()).toContain('Gallina Feliz');
    expect(pantalla()).toContain('Fresco');
    expect(pantalla()).not.toContain('Cargando');
  });

  it('si no hay productos lo dice', async () => {
    http.expectOne('/api/products').flush([]);
    await fixture.whenStable();

    expect(pantalla()).toContain('Todavía no tienes productos');
  });

  it('si la API falla muestra el error y permite reintentar', async () => {
    http.expectOne('/api/products').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudieron cargar los productos');
    expect(pantalla()).toContain('Reintentar');
  });

  it('cada producto tiene un enlace para editarlo', async () => {
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    const enlace = (fixture.nativeElement as HTMLElement).querySelector('a[href="/productos/1/editar"]');
    expect(enlace).not.toBeNull();
  });

  it('al borrar (y confirmar) hace DELETE y quita el producto de la lista', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    fixture.componentInstance.borrar(huevos);

    const peticion = http.expectOne('/api/products/1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
    await fixture.whenStable();

    expect(pantalla()).not.toContain('Huevos L');
    expect(pantalla()).toContain('Todavía no tienes productos');
  });

  it('si el usuario cancela la confirmación no borra nada', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    fixture.componentInstance.borrar(huevos);

    http.expectNone('/api/products/1');
    expect(pantalla()).toContain('Huevos L');
  });


  it('"A Compra" hace POST con 1 envase y avisa de que se añadió', async () => {
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    fixture.componentInstance.aCompra(huevos);

    const peticion = http.expectOne('/api/shopping-list/items');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ productId: 1, packages: 1 });
    peticion.flush({});
    await fixture.whenStable();

    expect(pantalla()).toContain('"Huevos L" añadido a la compra');
  });

  it('si "A Compra" falla avisa y no esconde los productos', async () => {
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    fixture.componentInstance.aCompra(huevos);
    http.expectOne('/api/shopping-list/items').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudo añadir "Huevos L" a la compra');
    expect(pantalla()).toContain('Huevos L');
  });

  it('si el borrado falla avisa y mantiene el producto', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    http.expectOne('/api/products').flush([huevos]);
    await fixture.whenStable();

    fixture.componentInstance.borrar(huevos);
    http.expectOne('/api/products/1').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(pantalla()).toContain('No se pudo borrar "Huevos L"');
    expect(pantalla()).toContain('Huevos L');
  });


});
