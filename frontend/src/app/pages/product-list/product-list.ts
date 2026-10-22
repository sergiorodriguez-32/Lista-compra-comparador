import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CATEGORIAS, ProductResponse, Products, UNIDADES } from '../../core/products';

@Component({
  selector: 'app-product-list',
  // Cada pipe y cada módulo que usa la plantilla hay que importarlo aquí (componentes standalone)
  imports: [RouterLink, MatButtonModule, MatCardModule, CurrencyPipe, DecimalPipe],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private products = inject(Products);

  // Estado de la pantalla. Un signal es una "caja" que avisa a la plantilla cuando cambia su
  // contenido: se lee con lista() y se modifica con lista.set(...).
  lista = signal<ProductResponse[]>([]);
  cargando = signal(true);
  error = signal<string | null>(null);
  // Error al borrar: aparte de 'error' para no esconder la lista entera si falla un borrado
  errorBorrado = signal<string | null>(null);

  // La plantilla (HTML) solo ve lo que es público en la clase
  readonly categorias = CATEGORIAS;
  readonly unidades = UNIDADES;

  // ngOnInit lo llama Angular una vez, justo después de crear el componente: aquí se cargan los datos
  ngOnInit() {
    this.cargar();
  }

  borrar(producto: ProductResponse) {
    // confirm() es el cuadro nativo del navegador: devuelve true si pulsa "Aceptar"
    if (!confirm(`¿Borrar "${producto.name}"?`)) return;
    this.errorBorrado.set(null);

    this.products.delete(producto.id).subscribe({
      // No hace falta volver a pedir la lista: se quita de la que ya tenemos
      next: () => this.lista.update(lista => lista.filter(p => p.id !== producto.id)),
      error: () => this.errorBorrado.set(`No se pudo borrar "${producto.name}"`),
    });
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);

    // subscribe es lo que envía la petición. Se pasa un objeto con dos caminos:
    // next (llegó la respuesta) y error (falló: sin conexión, 401, 500...)
    this.products.list().subscribe({
      next: productos => {
        this.lista.set(productos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los productos');
        this.cargando.set(false);
      },
    });
  }
}
