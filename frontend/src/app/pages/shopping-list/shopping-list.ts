import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ProductResponse, Products } from '../../core/products';
import { ShoppingItemResponse, ShoppingList, ShoppingListResponse } from '../../core/shopping-list';

@Component({
  selector: 'app-shopping-list',
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatCheckboxModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, DecimalPipe,
  ],
  templateUrl: './shopping-list.html',
  styleUrl: './shopping-list.scss',
})
export class ShoppingListPage implements OnInit {
  private shopping = inject(ShoppingList);
  private products = inject(Products);
  private fb = inject(FormBuilder);
  private formDir = viewChild.required(FormGroupDirective);

  // La API devuelve siempre la lista entera: cada respuesta sustituye a la anterior
  lista = signal<ShoppingListResponse | null>(null);
  items = computed(() => this.lista()?.items ?? []);
  productos = signal<ProductResponse[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  errorAccion = signal<string | null>(null);
  anadiendo = signal(false);

  // Mismos límites que ShoppingItemRequest.java: producto obligatorio, envases enteros 1-999
  form = this.fb.group({
    productId: this.fb.control<number | null>(null, Validators.required),
    packages: this.fb.control<number | null>(1, [
      Validators.required, Validators.min(1), Validators.max(999), Validators.pattern(/^\d+$/),
    ]),
  });

  ngOnInit() {
    this.cargar();
    this.products.list().subscribe({ next: productos => this.productos.set(productos) });
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);

    this.shopping.get().subscribe({
      next: lista => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de la compra');
        this.cargando.set(false);
      },
    });
  }

  anadir() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.anadiendo.set(true);
    this.errorAccion.set(null);

    const { productId, packages } = this.form.getRawValue();
    this.shopping.add({ productId: productId!, packages: packages! }).subscribe({
      next: lista => {
        this.lista.set(lista);
        this.formDir().resetForm({ productId: null, packages: 1 });
        this.anadiendo.set(false);
      },
      error: () => {
        this.errorAccion.set('No se pudo añadir a la lista');
        this.anadiendo.set(false);
      },
    });
  }

  /** Suma o resta envases (delta = +1 o -1). El mínimo es 1; para quitar, está "Quitar". */
  cambiar(item: ShoppingItemResponse, delta: number) {
    const nuevos = item.packages + delta;
    if (nuevos < 1 || nuevos > 999) return;
    this.errorAccion.set(null);

    this.shopping.updatePackages(item.id, nuevos).subscribe({
      next: lista => this.lista.set(lista),
      error: () => this.errorAccion.set(`No se pudo cambiar "${item.product.name}"`),
    });
  }

  marcar(item: ShoppingItemResponse, checked: boolean) {
    this.errorAccion.set(null);

    this.shopping.setChecked(item.id, checked).subscribe({
      next: lista => this.lista.set(lista),
      error: () => this.errorAccion.set(`No se pudo marcar "${item.product.name}"`),
    });
  }

  borrar(item: ShoppingItemResponse) {
    if (!confirm(`¿Quitar "${item.product.name}" de la lista?`)) return;
    this.errorAccion.set(null);

    this.shopping.delete(item.id).subscribe({
      next: lista => this.lista.set(lista),
      error: () => this.errorAccion.set(`No se pudo quitar "${item.product.name}"`),
    });
  }
}
