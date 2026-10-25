import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ProductResponse } from './products';

/** Lo que devuelve la API (equivale a PantryItemResponse.java). El producto va dentro. */
export interface PantryItemResponse {
  id: number;
  product: ProductResponse;
  // En la unidad base del producto (huevos, kg, litros), no en envases
  quantity: number;
}

/** Para añadir un producto a la despensa (equivale a PantryItemRequest.java). */
export interface PantryItemRequest {
  productId: number;
  quantity: number;
}

/**
 * Habla con /api/pantry. Igual que Products: cada método devuelve un Observable que no envía
 * nada hasta que alguien hace subscribe(), y el authInterceptor pone el token solo.
 */
@Injectable({ providedIn: 'root' })
export class Pantry {
  private http = inject(HttpClient);
  private readonly url = '/api/pantry';

  list() {
    return this.http.get<PantryItemResponse[]>(this.url);
  }

  // Si el producto ya estaba en la despensa, el backend suma la cantidad al item existente
  add(datos: PantryItemRequest) {
    return this.http.post<PantryItemResponse>(this.url, datos);
  }

  // PATCH porque solo cambia un campo; la cantidad nueva reemplaza a la anterior
  updateQuantity(id: number, quantity: number) {
    return this.http.patch<PantryItemResponse>(`${this.url}/${id}`, { quantity });
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
