import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

// Los valores permitidos son los mismos que los enum de Java (PackageUnit, ProductCategory y
// ProductSource). TypeScript los comprueba al compilar: escribir 'PIZZA' daría error aquí,
// antes incluso de llegar al backend.
export type PackageUnit = 'UNIDAD' | 'KG' | 'LITRO' | 'GRAMO' | 'ML';
export type ProductCategory = 'SECO' | 'CONGELADO' | 'FRESCO' | 'FRUTA' | 'DROGUERIA';
export type ProductSource = 'MANUAL' | 'MERCADONA' | 'ALCAMPO' | 'AHORRAMAS';

// Textos para mostrar al usuario. Record<ProductCategory, string> obliga a tener una entrada
// por cada categoría: si algún día se añade una al tipo y se olvida aquí, no compila.
export const CATEGORIAS: Record<ProductCategory, string> = {
  SECO: 'Seco',
  CONGELADO: 'Congelado',
  FRESCO: 'Fresco',
  FRUTA: 'Fruta',
  DROGUERIA: 'Droguería',
};

export const UNIDADES: Record<PackageUnit, string> = {
  UNIDAD: 'unidades',
  KG: 'kg',
  LITRO: 'L',
  GRAMO: 'g',
  ML: 'ml',
};

/** Lo que devuelve la API (equivale a ProductResponse.java). */
export interface ProductResponse {
  id: number;
  source: ProductSource;
  name: string;
  brand: string | null;
  description: string | null;
  price: number;
  packageAmount: number;
  packageUnit: PackageUnit;
  category: ProductCategory;
  ingredients: string | null;
}

/**
 * Lo que se envía para crear o editar (equivale a ProductRequest.java). No lleva id ni
 * source ni dueño: los pone el servidor, igual que en el backend.
 */
export interface ProductRequest {
  name: string;
  brand: string | null;
  description: string | null;
  price: number;
  packageAmount: number;
  packageUnit: PackageUnit;
  category: ProductCategory;
  ingredients: string | null;
}

/**
 * Habla con /api/products. Cada método devuelve un Observable: la petición NO se envía hasta
 * que alguien hace subscribe() (igual que en Auth). No hace falta pasar el token aquí: el
 * authInterceptor lo añade solo a todas las peticiones.
 */
@Injectable({ providedIn: 'root' })
export class Products {
  private http = inject(HttpClient);
  private readonly url = '/api/products';

  list() {
    return this.http.get<ProductResponse[]>(this.url);
  }

  get(id: number) {
    return this.http.get<ProductResponse>(`${this.url}/${id}`);
  }

  create(datos: ProductRequest) {
    return this.http.post<ProductResponse>(this.url, datos);
  }

  update(id: number, datos: ProductRequest) {
    return this.http.put<ProductResponse>(`${this.url}/${id}`, datos);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
