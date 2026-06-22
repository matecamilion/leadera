import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { FotosPropiedadComponent } from '../../components/fotos-propiedad/fotos-propiedad';
import { MatchingService, CompradorPotencial } from '../../core/services/matching-service';
import { OperacionService, Operacion } from '../../core/services/operacion-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';

@Component({
  selector: 'app-propiedad-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, FotosPropiedadComponent],
  templateUrl: './propiedad-detalle.html',
  styleUrl: './propiedad-detalle.css'
})
export class PropiedadDetalle implements OnInit {
  private route = inject(ActivatedRoute);
  private propiedadService = inject(PropiedadService);
  private matchingService = inject(MatchingService);
  private operacionService = inject(OperacionService);
  private leadService = inject(LeadService);

  propiedad = signal<Propiedad | null>(null);
  operacionesVinculadas = signal<Operacion[]>([]);
  leadPropietario = signal<Lead | null>(null);

  // Matching de compradores potenciales
  compradores = signal<CompradorPotencial[]>([]);
  cargandoMatch = signal<boolean>(false);
  panelAbierto = signal<boolean>(false);
  errorMatch = signal<string>('');

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    const id = Number(idParam);
    if (!id || Number.isNaN(id)) return;

    this.propiedadService.obtenerPorId(id).subscribe({
      next: (p) => {
        this.propiedad.set(p);

        this.operacionService.obtenerOperacionesDePropiedad(p.id).subscribe({
          next: (ops) => this.operacionesVinculadas.set(ops),
          error: () => {}
        });

        if (p.leadId) {
          this.leadService.getLeadById(p.leadId).subscribe({
            next: (lead) => this.leadPropietario.set(lead),
            error: () => {}
          });
        }

        if (p.estado === 'DISPONIBLE') {
          this.togglePanelCompradores();
        }
      }
    });
  }

  cambiarEstado(estado: string) {
    const propiedadActual = this.propiedad();
    if (!propiedadActual) return;

    this.propiedadService.actualizarEstado(propiedadActual.id, estado).subscribe({
      next: () => this.propiedad.update(p => ({ ...p!, estado: estado as any }))
    });
  }

  togglePanelCompradores() {
    if (this.panelAbierto()) {
      this.panelAbierto.set(false);
      this.compradores.set([]);
      this.errorMatch.set('');
      return;
    }

    const p = this.propiedad();
    if (!p) return;

    this.panelAbierto.set(true);
    this.cargandoMatch.set(true);
    this.errorMatch.set('');

    this.matchingService.obtenerCompradores(p.id).subscribe({
      next: (lista) => {
        this.compradores.set(lista);
        this.cargandoMatch.set(false);
      },
      error: (err) => {
        this.cargandoMatch.set(false);
        this.errorMatch.set(err?.error?.message ?? 'No se pudieron cargar los compradores potenciales.');
      },
    });
  }

  iniciales(lead: Lead | null): string {
    if (!lead) return '?';
    return `${lead.nombre?.charAt(0) ?? ''}${lead.apellido?.charAt(0) ?? ''}`.toUpperCase();
  }
}
