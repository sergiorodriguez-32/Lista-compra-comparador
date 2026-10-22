import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  CATEGORIAS, PackageUnit, ProductCategory, ProductRequest, Products, UNIDADES,
} from '../../core/products';

/**
 * Formulario para crear un producto (/productos/nuevo) o editarlo (/productos/:id/editar).
 * Es el mismo componente para las dos cosas: la única diferencia es si la ruta trae un id.
 */
@Component({
  selector: 'app-product-form',
  imports: [
    ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule,
  ],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductForm implements OnInit {
  private fb = inject(FormBuilder);
  private products = inject(Products);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** id del producto que se edita; null si se está creando uno nuevo */
  id = signal<number | null>(null);
  guardando = signal(false);
  error = signal<string | null>(null);

  // Las listas del desplegable salen de los mismos mapas que usa la pantalla de lista
  readonly categorias = Object.entries(CATEGORIAS)
    .map(([valor, texto]) => ({ valor: valor as ProductCategory, texto }));
  readonly unidades = Object.entries(UNIDADES)
    .map(([valor, texto]) => ({ valor: valor as PackageUnit, texto }));

  // Los límites son los mismos que los de ProductRequest.java (@Size, @DecimalMin...): el
  // formulario avisa antes de enviar, pero el backend sigue validando (no te puedes fiar
  // de lo que llega del navegador).
  form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    brand: this.fb.nonNullable.control('', Validators.maxLength(100)),
    description: this.fb.nonNullable.control('', Validators.maxLength(2000)),
    // Los números empiezan en null (campo vacío) en vez de 0, para que "required" funcione
    price: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    packageAmount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
    packageUnit: this.fb.nonNullable.control<PackageUnit>('UNIDAD'),
    category: this.fb.nonNullable.control<ProductCategory>('SECO'),
    ingredients: this.fb.nonNullable.control('', Validators.maxLength(5000)),
  });

  ngOnInit() {
    // snapshot = la ruta tal como era al abrir la pantalla. En /productos/7/editar, id vale "7"
    const idRuta = this.route.snapshot.paramMap.get('id');
    if (idRuta) {
      this.id.set(Number(idRuta));
      this.cargarProducto(Number(idRuta));
    }
  }

  private cargarProducto(id: number) {
    this.products.get(id).subscribe({
      // patchValue rellena el formulario. La API devuelve null en los campos vacíos, pero el
      // <input> necesita texto, de ahí el "?? ''" (si es null, usa '').
      next: p => this.form.patchValue({
        ...p,
        brand: p.brand ?? '',
        description: p.description ?? '',
        ingredients: p.ingredients ?? '',
      }),
      error: () => this.error.set('No se pudo cargar el producto'),
    });
  }

  enviar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // hace que se vean los errores de los campos tocados o no
      return;
    }
    this.guardando.set(true);
    this.error.set(null);

    const v = this.form.getRawValue();
    const datos: ProductRequest = {
      name: v.name.trim(),
      // Un texto vacío se envía como null: así lo guarda la base de datos para "sin dato"
      brand: v.brand.trim() || null,
      description: v.description.trim() || null,
      // El formulario ya validó que no son null (required), por eso el "!"
      price: v.price!,
      packageAmount: v.packageAmount!,
      packageUnit: v.packageUnit,
      category: v.category,
      ingredients: v.ingredients.trim() || null,
    };

    const id = this.id();
    const peticion = id === null ? this.products.create(datos) : this.products.update(id, datos);

    peticion.subscribe({
      next: () => this.router.navigate(['/productos']),
      error: e => {
        // Si el backend rechaza los datos, responde 400 con { detail: "..." } (ProblemDetail)
        this.error.set(e.error?.detail ?? 'No se pudo guardar el producto');
        this.guardando.set(false);
      },
    });
  }
}
