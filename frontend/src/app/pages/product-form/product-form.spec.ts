import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ProductResponse } from '../../core/products';
import { ProductForm } from './product-form';

describe('ProductForm', () => {
  let fixture: ComponentFixture<ProductForm>;
  let component: ProductForm;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  const huevos: ProductResponse = {
    id: 7, source: 'MANUAL', name: 'Huevos L', brand: null, description: null,
    price: 2.5, packageAmount: 12, packageUnit: 'UNIDAD', category: 'FRESCO', ingredients: null,
  };

  /** Monta el componente. idRuta simula la ruta: '7' = /productos/7/editar, null = /productos/nuevo */
  async function montar(idRuta: string | null) {
    await TestBed.configureTestingModule({
      imports: [ProductForm],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        // ActivatedRoute falso: solo lo que lee el componente (route.snapshot.paramMap.get('id'))
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(idRuta ? { id: idRuta } : {}) } } },
      ],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ProductForm);
    component = fixture.componentInstance;
    fixture.detectChanges(); // ejecuta ngOnInit
  }

  afterEach(() => http.verify());

  describe('crear (/productos/nuevo)', () => {
    beforeEach(() => montar(null));

    it('no envía nada si el formulario no es válido', () => {
      component.enviar(); // vacío: falta nombre, precio y contenido

      expect(component.form.invalid).toBe(true);
      http.expectNone('/api/products'); // además, afterEach(verify) comprueba que no salió nada
    });

    it('envía POST con los datos y vuelve a la lista', () => {
      component.form.patchValue({
        name: '  Leche entera ', brand: 'Pascual', price: 0.99, packageAmount: 1,
        packageUnit: 'LITRO', category: 'FRESCO',
      });

      component.enviar();

      const peticion = http.expectOne('/api/products');
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        name: 'Leche entera', // sin los espacios de los lados
        brand: 'Pascual',
        description: null, // el texto vacío se envía como null
        price: 0.99,
        packageAmount: 1,
        packageUnit: 'LITRO',
        category: 'FRESCO',
        ingredients: null,
      });
      peticion.flush({ ...huevos, id: 8 });

      expect(navegar).toHaveBeenCalledWith(['/productos']);
    });

    it('si el backend rechaza los datos muestra su mensaje y no navega', async () => {
      component.form.patchValue({ name: 'X', price: 1, packageAmount: 1 });
      component.enviar();

      http.expectOne('/api/products').flush(
        { detail: 'Datos de entrada no válidos' }, { status: 400, statusText: 'Bad Request' });
      await fixture.whenStable();

      expect(component.error()).toBe('Datos de entrada no válidos');
      expect(component.guardando()).toBe(false); // se puede volver a intentar
      expect(navegar).not.toHaveBeenCalled();
    });
  });

  describe('editar (/productos/7/editar)', () => {
    beforeEach(() => montar('7'));

    it('carga el producto y rellena el formulario', () => {
      http.expectOne('/api/products/7').flush(huevos);

      expect(component.id()).toBe(7);
      expect(component.form.getRawValue().name).toBe('Huevos L');
      expect(component.form.getRawValue().brand).toBe(''); // null de la API -> '' en el input
      expect(component.form.getRawValue().price).toBe(2.5);
    });

    it('envía PUT a /api/products/7 al guardar', () => {
      http.expectOne('/api/products/7').flush(huevos);
      component.form.patchValue({ name: 'Huevos XL' });

      component.enviar();

      const peticion = http.expectOne('/api/products/7');
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body.name).toBe('Huevos XL');
      peticion.flush({ ...huevos, name: 'Huevos XL' });

      expect(navegar).toHaveBeenCalledWith(['/productos']);
    });

    it('si el producto no se puede cargar, avisa', () => {
      http.expectOne('/api/products/7').flush(null, { status: 404, statusText: 'Not Found' });

      expect(component.error()).toBe('No se pudo cargar el producto');
    });
  });
});
