import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import {
  OperacionService,
  Operacion,
  CrearOperacionRequest
} from '../../core/services/operacion-service';

@Component({
  selector: 'app-gestionar-busqueda',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './gestionar-busqueda.html',
  styleUrl: './gestionar-busqueda.css'
})
export class GestionarBusquedaComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private operacionService = inject(OperacionService);

  public busquedaForm: FormGroup;
  public leadId!: number;
  public cargando = false;

  private operacionCompraExistente?: Operacion;

  constructor() {
    this.busquedaForm = this.fb.group({
      precioMin: [0, [Validators.min(0)]],
      precioMax: [null, [Validators.required, Validators.min(0)]],
      cantidadAmbientes: [null, [Validators.required, Validators.min(1)]],
      metrosTotales: [null, [Validators.required, Validators.min(1)]],
      metrosCubiertos: [null, [Validators.min(0)]],
      metrosDescubiertos: [null, [Validators.min(0)]],
      tipoVivienda: ['', Validators.required],
      zona: ['', Validators.required],
      observaciones: ['']
    }, {
      validators: [this.verificarRangosLogicos]
    });

    this.setupSuscripcionesMetros();
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam) {
      this.leadId = Number(idParam);
      this.cargarDatosExistentes();
    }
  }

  private cargarDatosExistentes(): void {
    this.cargando = true;

    this.operacionService.obtenerOperacionesDelLead(this.leadId).subscribe({
      next: (operaciones: Operacion[]) => {
        const operacionCompra = operaciones.find(
          operacion => operacion.tipoOperacion === 'COMPRA'
        );

        this.operacionCompraExistente = operacionCompra;

        if (operacionCompra?.busqueda) {
          this.busquedaForm.patchValue(operacionCompra.busqueda);
        }

        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al cargar las operaciones del lead', err);
        this.cargando = false;
      }
    });
  }

  private verificarRangosLogicos(control: AbstractControl): ValidationErrors | null {
    const pMin = control.get('precioMin')?.value;
    const pMax = control.get('precioMax')?.value;
    const mCub = control.get('metrosCubiertos')?.value;
    const mTot = control.get('metrosTotales')?.value;

    const errors: any = {};

    if (pMin !== null && pMax !== null && pMin > pMax) {
      errors['precioInconsistente'] = true;
    }

    if (mCub !== null && mTot !== null && mCub > mTot) {
      errors['metrosInconsistentes'] = true;
    }

    return Object.keys(errors).length ? errors : null;
  }

  private setupSuscripcionesMetros(): void {
    this.busquedaForm.valueChanges.subscribe(val => {
      const totales = val.metrosTotales || 0;
      const cubiertos = val.metrosCubiertos || 0;
      const descubiertosCalculados = totales - cubiertos;

      if (
        descubiertosCalculados >= 0 &&
        val.metrosDescubiertos !== descubiertosCalculados
      ) {
        this.busquedaForm
          .get('metrosDescubiertos')
          ?.setValue(descubiertosCalculados, { emitEvent: false });
      }
    });
  }

  guardar(): void {
    if (this.busquedaForm.invalid) {
      this.busquedaForm.markAllAsTouched();
      return;
    }

    const datosBusqueda = this.busquedaForm.value;

    const nuevaOperacion: CrearOperacionRequest = {
      titulo: 'Búsqueda de compra',
      tipoOperacion: 'COMPRA',
      descripcion: datosBusqueda.observaciones || 'Búsqueda cargada para el lead',
      propiedad: null,
      busqueda: datosBusqueda
    };

    this.operacionService.crearOperacion(this.leadId, nuevaOperacion).subscribe({
      next: () => {
        this.router.navigate(['/leads', this.leadId]);
      },
      error: (err: any) => {
        alert('Hubo un error al guardar la búsqueda. Intenta nuevamente.');
        console.error(err);
      }
    });
  }

  get f() {
    return this.busquedaForm.controls;
  }
}