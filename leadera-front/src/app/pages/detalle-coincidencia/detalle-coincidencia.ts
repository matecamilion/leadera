import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PropiedadService } from '../../core/services/propiedad-service';
import { Propiedad } from '../../core/models/propiedad';
import { MatchingService, CompradorPotencial } from '../../core/services/matching-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';

// Espejo del backend PropiedadService.coincide: zona contains bidireccional
// case-insensitive, tipo igual, precio dentro de [min,max], ambientes y
// metros propiedad >= búsqueda. Criterio sin valor en la búsqueda = sin
// preferencia (no participa del match).
interface CriterioComparado {
  label: string;
  valorPropiedad: string;
  valorBusqueda: string;
  estado: 'cumple' | 'no-cumple' | 'sin-preferencia';
}

@Component({
  selector: 'app-detalle-coincidencia',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './detalle-coincidencia.html',
  styleUrl: './detalle-coincidencia.css',
})
export class DetalleCoincidencia implements OnInit {
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private propiedadService = inject(PropiedadService);
  private matchingService = inject(MatchingService);
  private leadService = inject(LeadService);

  propiedad = signal<Propiedad | null>(null);
  comprador = signal<CompradorPotencial | null>(null);
  lead = signal<Lead | null>(null);
  cargando = signal<boolean>(true);
  noVigente = signal<boolean>(false);
  error = signal<string>('');

  ngOnInit() {
    const propiedadId = Number(this.route.snapshot.paramMap.get('propiedadId'));
    const operacionId = Number(this.route.snapshot.paramMap.get('operacionId'));
    if (!propiedadId || !operacionId || Number.isNaN(propiedadId) || Number.isNaN(operacionId)) {
      this.cargando.set(false);
      this.error.set('Coincidencia inválida.');
      return;
    }

    forkJoin({
      propiedad: this.propiedadService.obtenerPorId(propiedadId),
      compradores: this.matchingService.obtenerCompradores(propiedadId),
    }).subscribe({
      next: ({ propiedad, compradores }) => {
        this.propiedad.set(propiedad);
        const c = compradores.find((x) => x.operacionId === operacionId) ?? null;
        if (!c) {
          this.noVigente.set(true);
          this.cargando.set(false);
          return;
        }
        this.comprador.set(c);
        if (c.esMio) {
          // Solo leads propios: el backend no expone contacto de leads ajenos.
          this.leadService.getLeadById(c.leadId).subscribe({
            next: (lead) => {
              this.lead.set(lead);
              this.cargando.set(false);
            },
            error: () => this.cargando.set(false),
          });
        } else {
          this.cargando.set(false);
        }
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo cargar la coincidencia.');
      },
    });
  }

  criterios = computed<CriterioComparado[]>(() => {
    const p = this.propiedad();
    const c = this.comprador();
    if (!p || !c) return [];
    const fmt = (n: number) => n.toLocaleString('en-US');
    const rows: CriterioComparado[] = [];

    const zp = (p.zona ?? '').toLowerCase().trim();
    const zb = (c.busquedaZona ?? '').toLowerCase().trim();
    rows.push({
      label: 'Zona',
      valorPropiedad: p.zona || '—',
      valorBusqueda: c.busquedaZona || 'Sin preferencia',
      estado: !zb
        ? 'sin-preferencia'
        : zp && (zp.includes(zb) || zb.includes(zp))
          ? 'cumple'
          : 'no-cumple',
    });

    rows.push({
      label: 'Tipo',
      valorPropiedad: p.tipoVivienda || '—',
      valorBusqueda: c.busquedaTipoVivienda || 'Sin preferencia',
      estado: !c.busquedaTipoVivienda
        ? 'sin-preferencia'
        : p.tipoVivienda?.toUpperCase() === c.busquedaTipoVivienda.toUpperCase()
          ? 'cumple'
          : 'no-cumple',
    });

    const min = c.busquedaPrecioMin;
    const max = c.busquedaPrecioMax;
    let rango = 'Sin preferencia';
    if (min && max) rango = `USD ${fmt(min)} – ${fmt(max)}`;
    else if (max) rango = `hasta USD ${fmt(max)}`;
    else if (min) rango = `desde USD ${fmt(min)}`;
    rows.push({
      label: 'Precio',
      valorPropiedad: p.precio != null ? `USD ${fmt(p.precio)}` : '—',
      valorBusqueda: rango,
      estado:
        !min && !max
          ? 'sin-preferencia'
          : p.precio == null
            ? 'no-cumple'
            : (!min || p.precio >= min) && (!max || p.precio <= max)
              ? 'cumple'
              : 'no-cumple',
    });

    rows.push({
      label: 'Ambientes',
      valorPropiedad: p.cantidadAmbientes != null ? `${p.cantidadAmbientes} amb.` : '—',
      valorBusqueda: c.busquedaAmbientes ? `${c.busquedaAmbientes}+ amb.` : 'Sin preferencia',
      estado: !c.busquedaAmbientes
        ? 'sin-preferencia'
        : p.cantidadAmbientes != null && p.cantidadAmbientes >= c.busquedaAmbientes
          ? 'cumple'
          : 'no-cumple',
    });

    rows.push({
      label: 'Metros',
      valorPropiedad: p.metrosTotales != null ? `${p.metrosTotales} m²` : '—',
      valorBusqueda: c.busquedaMetros ? `${c.busquedaMetros}+ m²` : 'Sin preferencia',
      estado: !c.busquedaMetros
        ? 'sin-preferencia'
        : p.metrosTotales != null && p.metrosTotales >= c.busquedaMetros
          ? 'cumple'
          : 'no-cumple',
    });

    return rows;
  });

  volver() {
    this.location.back();
  }

  /**
   * Normaliza a formato internacional argentino para wa.me: 549 + código de
   * área + número, sin 0 ni 15. Tolerante: si tras la limpieza no queda un
   * número local de 10 dígitos, devuelve los dígitos tal cual.
   */
  telefonoWhatsApp(raw: string): string {
    const digits = (raw ?? '').replace(/\D/g, '');
    if (!digits) return '';
    let d = digits;
    if (d.startsWith('00')) d = d.slice(2);
    if (d.startsWith('549')) d = d.slice(3);
    else if (d.startsWith('54')) d = d.slice(2);
    if (d.startsWith('0')) d = d.slice(1);
    // "15" de celular va después del código de área (2 a 4 dígitos)
    if (d.length > 10) d = d.replace(/^(\d{2,4})15/, '$1');
    return d.length === 10 ? `549${d}` : digits;
  }

  linkWhatsApp(): string {
    const l = this.lead();
    const p = this.propiedad();
    if (!l || !p) return '';
    const tel = this.telefonoWhatsApp(l.telefono);
    const msg =
      `Hola ${l.nombre}! Te escribo porque tengo una propiedad en ${p.zona} ` +
      `que encaja con lo que estás buscando: ${p.direccion}. ` +
      `¿Te interesa que coordinemos una visita?`;
    return `https://wa.me/${tel}?text=${encodeURIComponent(msg)}`;
  }
}
