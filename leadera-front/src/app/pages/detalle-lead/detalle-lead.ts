import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { NotificationService } from '../../core/services/notification-service';
import { FormsModule } from '@angular/forms';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { Busqueda } from '../../core/models/busqueda';
import { PropiedadDetalle } from '../propiedad-detalle/propiedad-detalle';
import { OperacionService, Operacion, CrearOperacionRequest } from '../../core/services/operacion-service';
import { Lead } from '../../core/models/lead';

@Component({
  selector: 'app-detalle-lead',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, PropiedadDetalle],
  templateUrl: './detalle-lead.html',
  styleUrl: './detalle-lead.css'
})
export class DetalleLead implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private leadService = inject(LeadService);
  private notificationService = inject(NotificationService);
  private propiedadService = inject(PropiedadService);
  private operacionService = inject(OperacionService);

  public lead = signal<Lead | null>(null);
  public id: number = 0;
  operaciones = signal<Operacion[]>([]);
  errorGeneral = signal<string>('');
  tabActiva = signal<'operaciones' | 'interacciones' | 'propiedades'>('operaciones');

  ultimasOperaciones = computed(() => {
    const ops = this.operaciones();
    return ops.slice(-2).reverse();
  });

  ultimaInteraccion = computed(() => {
    const interacciones = this.lead()?.interacciones;
    if (!interacciones || interacciones.length === 0) return null;
    return interacciones[interacciones.length - 1];
  });

  // Contacto
  nuevoTelefono: string = '';
  nuevoEmail: string = '';
  errorContacto: string = '';

  // Propiedades
  propiedades = signal<Propiedad[]>([]);
  propiedadSeleccionada = signal<Propiedad | null>(null);
  nuevaPropiedad: Partial<Propiedad> = {};

  ngOnInit(): void {
    const paramsId = this.route.snapshot.paramMap.get('id');
    if (paramsId) {
      this.id = Number(paramsId);
      this.cargarDetalleLead();
    }
  }

  cargarDetalleLead() {
    this.leadService.getLeadById(this.id).subscribe({
      next: (data) => {
        this.lead.set(data);
        this.cargarPropiedades();
        this.cargarOperaciones();
      },
      error: (err) => this.errorGeneral.set(err.mensajeAmigable || 'No se pudo cargar el detalle del lead.')
    });
  }

  cargarPropiedades() {
  this.propiedadService.obtenerPorLead(this.id).subscribe({
    next: (data) => this.propiedades.set(data),
    error: (err) => this.errorGeneral.set(err.mensajeAmigable || 'No se pudieron cargar las propiedades.')
  });
}

  cargarOperaciones() {
  this.operacionService.obtenerOperacionesDelLead(this.id).subscribe({
    next: (data) => this.operaciones.set(data),
    error: (err) => this.errorGeneral.set(err.mensajeAmigable || 'No se pudieron cargar las operaciones.')
  });
}

  iniciales(nombre: string, apellido: string): string {
    return `${nombre?.charAt(0) ?? ''}${apellido?.charAt(0) ?? ''}`.toUpperCase();
  }

  claseAvatar(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'CALIENTE': return 'av-caliente';
      case 'TIBIO':    return 'av-tibio';
      case 'FRIO':     return 'av-frio';
      default:         return 'av-inactivo';
    }
  }

  agregarPropiedad(modal: HTMLDialogElement) {
  const propiedadAEnviar: any = { ...this.nuevaPropiedad };

  this.propiedadService.agregar(this.id, propiedadAEnviar).subscribe({
    next: (p) => {
      this.propiedades.update(list => [...list, p]);
      this.nuevaPropiedad = {};
      modal.close();
    },
    error: (err) => {
      this.errorGeneral.set(err.mensajeAmigable || 'No se pudo agregar la propiedad.');
    }
  });
}

  abrirModalContacto(modal: HTMLDialogElement) {
    const actual = this.lead();
    if (!actual) return;
    this.nuevoTelefono = actual.telefono ?? '';
    this.nuevoEmail = actual.email ?? '';
    this.errorContacto = '';
    modal.showModal();
  }

  guardarContacto(modal: HTMLDialogElement) {
    this.errorContacto = '';
    if (!this.nuevoTelefono?.trim()) {
      this.errorContacto = 'El teléfono es obligatorio.';
      return;
    }
    this.leadService.editarContacto(this.lead()!.id, this.nuevoTelefono, this.nuevoEmail).subscribe({
      next: (leadActualizado) => {
        this.lead.set(leadActualizado);
        modal.close();
      },
      error: (err) => {
        this.errorContacto = err.error || 'Ya existe un lead con ese teléfono o email.';
      }
    });
  }

  confirmarCambio(nuevoEstado: string, modal: HTMLDialogElement) {
    this.leadService.actualizarEstado(this.lead()!.id, nuevoEstado).subscribe({
      next: (leadActualizado) => {
        this.lead.set(leadActualizado);
        modal.close();
      },
      error: (err) => {
        this.errorGeneral.set(err.mensajeAmigable || 'No se pudo actualizar el estado.');
      }
    });
  }

  eliminarLead(modal: HTMLDialogElement) {
    this.leadService.eliminarLead(this.id).subscribe({
      next: () => {
        modal.close();
        this.notificationService.exito('Lead eliminado correctamente.');
        this.router.navigate(['/leads']);
      },
      error: () => modal.close()
    });
  }

  establecerLeadInactivo(modal: HTMLDialogElement) {
    this.leadService.establecerLeadInactivo(this.lead()!.id).subscribe({
      next: (leadActualizado) => {
        this.lead.set(leadActualizado);
        modal.close();
      },
      error: (err) => {
        this.errorGeneral.set(err.mensajeAmigable || 'No se pudo establecer como inactivo.');
      }
    });
  }
  nuevaOperacion: Partial<CrearOperacionRequest> = {};
  nuevaBusqueda: Partial<Busqueda> = {};
  errorOperacion: string = '';

  agregarOperacion(modal: HTMLDialogElement) {
    if (!this.nuevaOperacion.titulo || !this.nuevaOperacion.tipoOperacion) {
      this.errorOperacion = 'Título y tipo son obligatorios.';
      return;
    }
    if (this.nuevaOperacion.tipoOperacion === 'VENTA' && !this.nuevaOperacion.propiedad?.id) {
      this.errorOperacion = 'Seleccioná una propiedad para operaciones de venta.';
      return;
    }
    if (this.nuevaOperacion.tipoOperacion === 'COMPRA' && (!this.nuevaBusqueda.tipoVivienda || !this.nuevaBusqueda.zona)) {
      this.errorOperacion = 'Completá el tipo de vivienda y la zona de la búsqueda.';
      return;
    }

    const body: CrearOperacionRequest = {
      titulo: this.nuevaOperacion.titulo!,
      tipoOperacion: this.nuevaOperacion.tipoOperacion!,
      descripcion: this.nuevaOperacion.descripcion || '',
      propiedad: this.nuevaOperacion.tipoOperacion === 'VENTA' ? { id: this.nuevaOperacion.propiedad!.id } : null,
      busqueda: this.nuevaOperacion.tipoOperacion === 'COMPRA' ? (this.nuevaBusqueda as Busqueda) : null,
    };

    this.operacionService.crearOperacion(this.id, body).subscribe({
      next: (op) => {
        this.operaciones.update(list => [...list, op]);
        this.nuevaOperacion = {};
        this.nuevaBusqueda = {};
        this.errorOperacion = '';
        modal.close();
      },
      error: (err) => {
        this.errorOperacion = err.mensajeAmigable || 'Error al crear la operación.';
        this.errorGeneral.set(err.mensajeAmigable || 'No se pudo crear la operación.');
      }
    });
  }
}