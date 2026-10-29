import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ProductResponse } from './products';

/** Un item de la lista (equivale a ShoppingItemResponse.java). El subtotal ya viene calculado. */
export interface ShoppingItemResponse {
  id: number;
  product: ProductResponse;
  // Envases enteros, no unidad base (a diferencia de la despensa)
  packages: number;
  checked: boolean;
  subtotal: number;
}

/** La lista entera con el total estimado (equivale a ShoppingListResponse.java). */
export interface ShoppingListResponse {
  id: number;
  name: string;
  defaultList: boolean;
  items: ShoppingItemResponse[];
  estimatedTotal: number;
}

/** Para añadir un producto a la lista (equivale a ShoppingItemRequest.java). */
export interface ShoppingItemRequest {
  productId: number;
  packages: number;
}

/**
 * Habla con /api/shopping-list. Todos los métodos devuelven la lista completa ya actualizada,
 * así que el componente solo tiene que quedarse con la última respuesta.
 */
@Injectable({ providedIn: 'root' })
export class ShoppingList {
  private http = inject(HttpClient);
  private readonly url = '/api/shopping-list';

  get() {
    return this.http.get<ShoppingListResponse>(this.url);
  }

  // Si el producto ya estaba en la lista, el backend suma los envases al item existente
  add(datos: ShoppingItemRequest) {
    return this.http.post<ShoppingListResponse>(`${this.url}/items`, datos);
  }

  updatePackages(id: number, packages: number) {
    return this.http.patch<ShoppingListResponse>(`${this.url}/items/${id}`, { packages });
  }

  setChecked(id: number, checked: boolean) {
    return this.http.patch<ShoppingListResponse>(`${this.url}/items/${id}/checked`, { checked });
  }

  // Devuelve 200 con la lista restante, no 204
  delete(id: number) {
    return this.http.delete<ShoppingListResponse>(`${this.url}/items/${id}`);
  }
}
