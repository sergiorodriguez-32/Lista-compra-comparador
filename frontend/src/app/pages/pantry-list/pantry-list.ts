import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Pantry, PantryItemResponse } from '../../core/pantry';
import { ProductResponse, Products, UNIDADES } from '../../core/products';


@Component({
  selector: 'app-pantry-list',
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, DecimalPipe,
  ],
  templateUrl: './pantry-list.html',
  styleUrl: './pantry-list.scss',
})
export class PantryList implements OnInit {
  private pantry = inject(Pantry);
  private products = inject(Products);
  private fb = inject(FormBuilder);
  // La directiva del <form>: permite resetear también el estado "enviado", no solo los valores
  private formDir = viewChild.required(FormGroupDirective);

  lista = signal<PantryItemResponse[]>([]);
  // Los productos del usuario, para el desplegable de "Añadir"
  productos = signal<ProductResponse[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  // Error al añadir, cambiar la cantidad o borrar: aparte de 'error' para no esconder la lista
  errorAccion = signal<string | null>(null);
  anadiendo = signal(false);

  readonly unidades = UNIDADES;

  // Mismos límites que PantryItemRequest.java: el producto es obligatorio y la cantidad no
  // puede ser negativa
  form = this.fb.group({
    productId: this.fb.control<number | null>(null, Validators.required),
    quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
  });

  ngOnInit() {
    this.cargar();
    // Si falla este, el desplegable queda vacío; la lista de la despensa sigue funcionando
    this.products.list().subscribe({ next: productos => this.productos.set(productos) });
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);

    this.pantry.list().subscribe({
      next: items => {
        this.lista.set(items);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la despensa');
        this.cargando.set(false);
      },
    });
  }

  anadir() {
    if (this.form.invalid) {
      // Marca los campos como "tocados" para que salgan los mensajes de error
      this.form.markAllAsTouched();
      return;
    }
    this.anadiendo.set(true);
    this.errorAccion.set(null);

    const { productId, quantity } = this.form.getRawValue();
    this.pantry.add({ productId: productId!, quantity: quantity! }).subscribe({
      next: item => {
        // Si el producto ya estaba, el servidor devuelve el mismo item con la cantidad sumada:
        // se sustituye. Si es nuevo, se añade al final.
        this.lista.update(lista =>
          lista.some(i => i.id === item.id)
            ? lista.map(i => (i.id === item.id ? item : i))
            : [...lista, item],
        );
        this.formDir().resetForm();
        this.anadiendo.set(false);
      },
      error: () => {
        this.errorAccion.set('No se pudo añadir a la despensa');
        this.anadiendo.set(false);
      },
    });
  }

  /** Suma o resta a la cantidad actual (delta = +1 o -1). Nunca baja de 0. */
  cambiar(item: PantryItemResponse, delta: number) {
    // Se redondea a 3 decimales para evitar restos de coma flotante (0.1 + 0.2 = 0.30000000000000004)
    const nueva = Math.max(0, Math.round((item.quantity + delta) * 1000) / 1000);
    if (nueva === item.quantity) return;
    this.errorAccion.set(null);

    this.pantry.updateQuantity(item.id, nueva).subscribe({
      next: actualizado =>
        this.lista.update(lista => lista.map(i => (i.id === actualizado.id ? actualizado : i))),
      error: () => this.errorAccion.set(`No se pudo cambiar "${item.product.name}"`),
    });
  }

  borrar(item: PantryItemResponse) {
    if (!confirm(`¿Quitar "${item.product.name}" de la despensa?`)) return;
    this.errorAccion.set(null);

    this.pantry.delete(item.id).subscribe({
      next: () => this.lista.update(lista => lista.filter(i => i.id !== item.id)),
      error: () => this.errorAccion.set(`No se pudo quitar "${item.product.name}"`),
    });
  }
}
