import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { FormsModule } from '@angular/forms';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { Busqueda } from '../../core/models/busqueda';
import { DetallePropiedadComponent } from '../detalle-propiedad/detalle-propiedad';
import { OperacionService, Operacion, CrearOperacionRequest } from '../../core/services/operacion-service';

@Component({
  selector: 'app-detalle-lead',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DetallePropiedadComponent],
  templateUrl: './detalle-lead.html',
  styleUrl: './detalle-lead.css'
})
export class DetalleLead implements OnInit {
  private route = inject(ActivatedRoute);
  private leadService = inject(LeadService);
  private propiedadService = inject(PropiedadService);
  private operacionService = inject(OperacionService);

  public lead = signal<any>(null);
  public id: number = 0;
  operaciones = signal<Operacion[]>([]);

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
      error: (err) => console.error('Error al traer el lead', err)
    });
  }

  cargarPropiedades() {
  this.propiedadService.obtenerPorLead(this.id).subscribe({
    next: (data) => this.propiedades.set(data),
    error: (err) => console.error('Error al cargar propiedades', err)
  });
}

  cargarOperaciones() {
  this.operacionService.obtenerOperacionesDelLead(this.id).subscribe({
    next: (data) => this.operaciones.set(data),
    error: (err) => console.error('Error al cargar operaciones', err)
  });
}

  agregarPropiedad(modal: HTMLDialogElement) {
  const propiedadAEnviar: any = { ...this.nuevaPropiedad };

  if (
    propiedadAEnviar.fechaPublicacion &&
    !propiedadAEnviar.fechaPublicacion.includes('T')
  ) {
    propiedadAEnviar.fechaPublicacion = propiedadAEnviar.fechaPublicacion + 'T00:00:00';
  }

  this.propiedadService.agregar(this.id, propiedadAEnviar).subscribe({
    next: (p) => {
      this.propiedades.update(list => [...list, p]);
      this.nuevaPropiedad = {};
      modal.close();
    },
    error: (err) => {
      console.error(err);
    }
  });
}

  guardarContacto(modal: HTMLDialogElement) {
    this.errorContacto = '';
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
        console.error(err);
        alert("No se pudo actualizar el estado");
      }
    });
  }

  establecerLeadInactivo(modal: HTMLDialogElement) {
    this.leadService.establecerLeadInactivo(this.lead()!.id).subscribe({
      next: (leadActualizado) => {
        this.lead.set(leadActualizado);
        modal.close();
      },
      error: (err) => {
        console.log(err);
        alert("No se pudo establecer como inactivo");
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
        this.errorOperacion = 'Error al crear la operación.';
        console.error(err);
      }
    });
  }
}