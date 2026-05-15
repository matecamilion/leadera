import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth-service';
import { AgenteService, ActividadReciente } from '../../core/services/agente-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';

interface OrigenMock {
  nombre: string;
  porcentaje: number;
  color: string;
}

interface DashboardStats {
  activos: number;
  calientes: number;
  tibios: number;
  frios: number;
  ganadosMes: number;
  nuevosDelMes: number;
  perdidos: number;
  interacciones7d: number;
  tasaConversion: number;
  tiempoRespuesta: number;
}

const META_MES = 12;
const DONUT_RADIUS = 56;

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {
  private authService = inject(AuthService);
  private agenteService = inject(AgenteService);
  private leadService = inject(LeadService);
  private router = inject(Router);

  stats = signal<DashboardStats | null>(null);
  leadsHoy = signal<Lead[]>([]);
  actividadReciente = signal<ActividadReciente[]>([]);

  periodoActivo = signal<'7d' | '30d' | '90d' | 'ano'>('30d');
  fechaExportacion = signal<string>('');

  agenteNombre = computed(() => this.authService.getNombreAgente() || 'Agente');
  iniciales = computed(() => {
    const n = this.agenteNombre();
    const parts = n.trim().split(/\s+/);
    const a = parts[0]?.[0] || '';
    const b = parts[1]?.[0] || '';
    return (a + b).toUpperCase() || 'A';
  });

  saludo = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 19) return 'Buenas tardes';
    return 'Buenas noches';
  });

  mesActual = new Date().toLocaleString('es-AR', { month: 'long' });
  diasRestantesMes = computed(() => {
    const hoy = new Date();
    const ultimoDia = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).getDate();
    return ultimoDia - hoy.getDate();
  });

  metaMes = META_MES;
  metaProgreso = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    return Math.min((s.ganadosMes / this.metaMes) * 100, 100);
  });

  ritmoNecesario = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    const faltan = Math.max(this.metaMes - s.ganadosMes, 0);
    const dias = Math.max(this.diasRestantesMes(), 1);
    return +(faltan / dias).toFixed(1);
  });

  // Donut de temperatura
  donutCircunferencia = 2 * Math.PI * DONUT_RADIUS;
  donutRadius = DONUT_RADIUS;

  totalTemperatura = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    return s.calientes + s.tibios + s.frios;
  });

  arcoCaliente = computed(() => this.arcoDe(this.stats()?.calientes ?? 0));
  arcoTibio = computed(() => this.arcoDe(this.stats()?.tibios ?? 0));
  arcoFrio = computed(() => this.arcoDe(this.stats()?.frios ?? 0));

  offsetTibio = computed(() => this.arcoCaliente());
  offsetFrio = computed(() => this.arcoCaliente() + this.arcoTibio());

  porcCaliente = computed(() => this.porcentajeDe(this.stats()?.calientes ?? 0));
  porcTibio = computed(() => this.porcentajeDe(this.stats()?.tibios ?? 0));
  porcFrio = computed(() => this.porcentajeDe(this.stats()?.frios ?? 0));

  // Embudo
  embudoEtapas = computed(() => {
    const s = this.stats();
    if (!s) return [];
    const contactados = s.activos;
    const calificados = s.calientes + s.tibios;
    const visita = s.calientes;
    const oferta = Math.round(s.calientes * 0.6);
    const cerrado = s.ganadosMes;
    const base = Math.max(contactados, 1);

    return [
      { num: '01', nombre: 'Contactados', valor: contactados },
      { num: '02', nombre: 'Calificados', valor: calificados },
      { num: '03', nombre: 'Visita agendada', valor: visita },
      { num: '04', nombre: 'Oferta', valor: oferta },
      { num: '05', nombre: 'Cerrado', valor: cerrado },
    ].map(e => ({
      ...e,
      ancho: Math.min(100, (e.valor / base) * 100),
      porc: Math.round((e.valor / base) * 100),
    }));
  });

  // Próximas acciones
  proximasAcciones = computed(() => {
    const leads = this.leadsHoy();
    return leads
      .map(l => ({ lead: l, score: this.calcularScore(l) }))
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);
  });

  // Origen de leads — mock. TODO: reemplazar con GET /leads/agente/{id}/origen
  origenesMock: OrigenMock[] = [
    { nombre: 'Zonaprop', porcentaje: 38, color: 'var(--brand)' },
    { nombre: 'WhatsApp directo', porcentaje: 24, color: 'var(--brand-deep)' },
    { nombre: 'Instagram', porcentaje: 18, color: 'var(--cool)' },
    { nombre: 'Referidos', porcentaje: 12, color: 'var(--warm)' },
    { nombre: 'Cartel en propiedad', porcentaje: 8, color: 'var(--ink-3)' },
  ];

  ngOnInit() {
    const id = this.authService.getIdAgente();
    if (id) {
      this.agenteService.getDashboardStats(id).subscribe(data => this.stats.set(data));
      this.agenteService.getActividadReciente(id).subscribe({
        next: data => this.actividadReciente.set(data),
        error: () => this.actividadReciente.set([]),
      });
    }
    this.leadService.getLeadsHoy().subscribe(data => {
      const combinados = [
        ...(data.prioritarios || []),
        ...(data.nuevosSinContacto || []),
        ...(data.seguimientosDeHoy || []),
      ];
      const seenIds = new Set<number>();
      const unicos = combinados.filter(l => {
        if (seenIds.has(l.id)) return false;
        seenIds.add(l.id);
        return true;
      });
      this.leadsHoy.set(unicos);
    });
  }

  setPeriodo(p: '7d' | '30d' | '90d' | 'ano') {
    this.periodoActivo.set(p);
  }

  exportarPdf() {
    const ahora = new Date();
    const fecha = ahora.toLocaleDateString('es-AR', { day: '2-digit', month: 'long', year: 'numeric' });
    const hora = ahora.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' });
    this.fechaExportacion.set(`${fecha} · ${hora} hs`);
    setTimeout(() => window.print(), 50);
  }

  calcularScore(lead: Lead): number {
    let score = 0;
    if (lead.estado === 'CALIENTE') score += 40;
    else if (lead.estado === 'TIBIO') score += 20;
    else score += 5;

    if (!lead.ultimoContacto) {
      score += 30;
    } else {
      const dias = (Date.now() - new Date(lead.ultimoContacto).getTime()) / 86400000;
      if (dias <= 1) score += 30;
      else if (dias <= 3) score += 20;
      else if (dias <= 7) score += 10;
    }
    return Math.min(score, 100);
  }

  getAccionLabel(lead: Lead): string {
    if (!lead.ultimoContacto) return 'Contactar';
    const dias = (Date.now() - new Date(lead.ultimoContacto).getTime()) / 86400000;
    if (lead.estado === 'CALIENTE' && dias < 1) return 'Preparar';
    if (lead.estado === 'CALIENTE') return 'Enviar';
    if (lead.estado === 'TIBIO' && dias > 4) return 'Reactivar';
    return 'Llamar';
  }

  getSubtituloLead(lead: Lead): string {
    if (!lead.ultimoContacto) return 'Nunca contactado';
    const dias = Math.floor((Date.now() - new Date(lead.ultimoContacto).getTime()) / 86400000);
    if (dias === 0) return 'Contactado hoy';
    if (dias === 1) return 'Hace 1 día';
    return `Hace ${dias} días`;
  }

  inicialesLead(lead: Lead): string {
    const n = (lead.nombre || '?')[0];
    const a = (lead.apellido || '')[0] || '';
    return (n + a).toUpperCase();
  }

  estadoClass(estado: string): string {
    if (estado === 'CALIENTE') return 'hot';
    if (estado === 'TIBIO') return 'warm';
    if (estado === 'FRIO') return 'cool';
    return 'cool';
  }

  estadoLabel(estado: string): string {
    if (estado === 'CALIENTE') return 'Caliente';
    if (estado === 'TIBIO') return 'Tibio';
    if (estado === 'FRIO') return 'Frío';
    return estado;
  }

  iconoActividad(tipo: string): { icon: string; tone: string } {
    switch (tipo) {
      case 'LLAMADA': return { icon: 'call', tone: 'cool' };
      case 'EMAIL': return { icon: 'mail', tone: 'cool' };
      case 'WHATSAPP': return { icon: 'chat', tone: 'brand' };
      case 'VISITA': return { icon: 'home_work', tone: 'brand' };
      case 'REUNION': return { icon: 'event', tone: 'warm' };
      default: return { icon: 'bolt', tone: 'cool' };
    }
  }

  formatRelativo(fechaIso: string): string {
    const diff = Date.now() - new Date(fechaIso).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return 'Justo ahora';
    if (min < 60) return `Hace ${min} min`;
    const h = Math.floor(min / 60);
    if (h < 24) return `Hace ${h} h`;
    const d = Math.floor(h / 24);
    if (d === 1) return 'Ayer';
    if (d < 7) return `Hace ${d} d`;
    return new Date(fechaIso).toLocaleDateString('es-AR', { day: '2-digit', month: 'short' });
  }

  irALead(id: number) {
    this.router.navigate(['/leads', id]);
  }

  private arcoDe(valor: number): number {
    const total = this.totalTemperatura();
    if (total === 0) return 0;
    return (valor / total) * this.donutCircunferencia;
  }

  private porcentajeDe(valor: number): number {
    const total = this.totalTemperatura();
    if (total === 0) return 0;
    return Math.round((valor / total) * 100);
  }
}
