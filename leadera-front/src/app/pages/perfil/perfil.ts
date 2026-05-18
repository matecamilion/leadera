import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth-service';
import {
  AgenteService,
  ActividadReciente,
  DashboardData,
  DashboardPeriodo,
} from '../../core/services/agente-service';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';

const DONUT_RADIUS = 56;
const ORIGEN_COLORS = [
  'var(--brand)',
  'var(--brand-deep)',
  'var(--cool)',
  'var(--warm)',
  'var(--ink-3)',
  'var(--ink-4)',
];

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

  dashboard = signal<DashboardData | null>(null);
  leadsHoy = signal<Lead[]>([]);
  actividadReciente = signal<ActividadReciente[]>([]);

  periodoActivo = signal<DashboardPeriodo>('30d');
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

  diasRestantesMes = computed(() => this.dashboard()?.snapshot.diasRestantesMes ?? 0);
  metaMes = computed(() => this.dashboard()?.snapshot.metaMensual ?? 12);

  metaProgreso = computed(() => {
    const s = this.dashboard()?.snapshot;
    if (!s) return 0;
    const meta = s.metaMensual || 1;
    return Math.min((s.ganadosMes / meta) * 100, 100);
  });

  ritmoNecesario = computed(() => {
    const s = this.dashboard()?.snapshot;
    if (!s) return 0;
    const faltan = Math.max(s.metaMensual - s.ganadosMes, 0);
    const dias = Math.max(s.diasRestantesMes, 1);
    return +(faltan / dias).toFixed(1);
  });

  // Donut de temperatura
  donutCircunferencia = 2 * Math.PI * DONUT_RADIUS;
  donutRadius = DONUT_RADIUS;

  totalTemperatura = computed(() => {
    const s = this.dashboard()?.snapshot;
    if (!s) return 0;
    return s.calientes + s.tibios + s.frios;
  });

  arcoCaliente = computed(() => this.arcoDe(this.dashboard()?.snapshot.calientes ?? 0));
  arcoTibio = computed(() => this.arcoDe(this.dashboard()?.snapshot.tibios ?? 0));
  arcoFrio = computed(() => this.arcoDe(this.dashboard()?.snapshot.frios ?? 0));

  offsetTibio = computed(() => this.arcoCaliente());
  offsetFrio = computed(() => this.arcoCaliente() + this.arcoTibio());

  porcCaliente = computed(() => this.porcentajeDe(this.dashboard()?.snapshot.calientes ?? 0));
  porcTibio = computed(() => this.porcentajeDe(this.dashboard()?.snapshot.tibios ?? 0));
  porcFrio = computed(() => this.porcentajeDe(this.dashboard()?.snapshot.frios ?? 0));

  // Embudo real desde backend
  embudoEtapas = computed(() => {
    const f = this.dashboard()?.snapshot.funnel;
    if (!f) return [];
    const base = Math.max(f.contactados, 1);
    const etapas = [
      { num: '01', nombre: 'Contactados', valor: f.contactados },
      { num: '02', nombre: 'Calificados', valor: f.calificados },
      { num: '03', nombre: 'Visita agendada', valor: f.visita },
      { num: '04', nombre: 'Oferta', valor: f.oferta },
      { num: '05', nombre: 'Cerrado', valor: f.cerrado },
    ];
    return etapas.map(e => ({
      ...e,
      ancho: Math.min(100, (e.valor / base) * 100),
      porc: Math.round((e.valor / base) * 100),
    }));
  });

  // Insight dinámico del embudo: mayor caída en %
  insightEmbudo = computed(() => {
    const etapas = this.embudoEtapas();
    if (etapas.length < 2) return null;
    let peorIdx = 1;
    let peorCaida = -1;
    for (let i = 1; i < etapas.length; i++) {
      const prev = etapas[i - 1].valor;
      const curr = etapas[i].valor;
      if (prev === 0) continue;
      const caida = (prev - curr) / prev;
      if (caida > peorCaida) {
        peorCaida = caida;
        peorIdx = i;
      }
    }
    if (peorCaida <= 0) return null;
    return {
      desde: etapas[peorIdx - 1].nombre,
      hasta: etapas[peorIdx].nombre,
      porc: Math.round(peorCaida * 100),
    };
  });

  origenes = computed(() => {
    const list = this.dashboard()?.origenes ?? [];
    return list.map((o, i) => ({ ...o, color: ORIGEN_COLORS[i % ORIGEN_COLORS.length] }));
  });

  // Sparklines (paths SVG)
  sparkActivos = computed(() => this.linePath(this.dashboard()?.kpis.leadsActivos.sparkline ?? [], 120, 32));
  sparkActivosArea = computed(() => this.areaPath(this.dashboard()?.kpis.leadsActivos.sparkline ?? [], 120, 32));
  sparkTasa = computed(() => this.linePath(this.dashboard()?.kpis.tasaConversion.sparkline ?? [], 120, 32));
  sparkTiempo = computed(() => this.linePath(this.dashboard()?.kpis.tiempoRespuesta.sparkline ?? [], 120, 32));
  sparkGanados = computed(() => this.linePath(this.dashboard()?.kpis.ganados.sparkline ?? [], 120, 32));
  sparkGanadosArea = computed(() => this.areaPath(this.dashboard()?.kpis.ganados.sparkline ?? [], 120, 32));

  // Evolución (líneas y área)
  private evolMaxY = computed(() => {
    const ev = this.dashboard()?.evolucion;
    if (!ev) return 1;
    const all = [...ev.nuevos, ...ev.ganados, ...ev.perdidos];
    return Math.max(1, ...all);
  });

  evolNuevosLinea = computed(() =>
    this.linePathScaled(this.dashboard()?.evolucion.nuevos ?? [], 720, 220, this.evolMaxY())
  );
  evolNuevosArea = computed(() =>
    this.areaPathScaled(this.dashboard()?.evolucion.nuevos ?? [], 720, 220, this.evolMaxY())
  );
  evolGanadosLinea = computed(() =>
    this.linePathScaled(this.dashboard()?.evolucion.ganados ?? [], 720, 220, this.evolMaxY())
  );
  evolPerdidosLinea = computed(() =>
    this.linePathScaled(this.dashboard()?.evolucion.perdidos ?? [], 720, 220, this.evolMaxY())
  );

  evolUltimoNuevoY = computed(() => {
    const arr = this.dashboard()?.evolucion.nuevos ?? [];
    if (arr.length === 0) return 110;
    const v = arr[arr.length - 1];
    const max = this.evolMaxY();
    return this.scaleY(v, max, 220);
  });

  totalNuevosPeriodo = computed(() => (this.dashboard()?.evolucion.nuevos ?? []).reduce((a, b) => a + b, 0));
  totalGanadosPeriodo = computed(() => (this.dashboard()?.evolucion.ganados ?? []).reduce((a, b) => a + b, 0));
  totalPerdidosPeriodo = computed(() => (this.dashboard()?.evolucion.perdidos ?? []).reduce((a, b) => a + b, 0));

  evolEjeX = computed(() => {
    const fechas = this.dashboard()?.evolucion.fechas ?? [];
    if (fechas.length === 0) return [];
    const idx = [0, Math.floor(fechas.length * 0.25), Math.floor(fechas.length * 0.5), Math.floor(fechas.length * 0.75)];
    return idx.map(i => this.formatFechaCorta(fechas[i]));
  });

  evolSubtitulo = computed(() => {
    const d = this.dashboard();
    if (!d) return '';
    if (d.periodo === 'ano') return 'Últimos 365 días';
    return `Últimos ${d.diasPeriodo} días`;
  });

  // Acceso rápido a KPIs para el template
  kpiActivos = computed(() => this.dashboard()?.kpis.leadsActivos);
  kpiTasa = computed(() => this.dashboard()?.kpis.tasaConversion);
  kpiTiempo = computed(() => this.dashboard()?.kpis.tiempoRespuesta);
  kpiGanados = computed(() => this.dashboard()?.kpis.ganados);

  // Próximas acciones
  proximasAcciones = computed(() => {
    const leads = this.leadsHoy();
    return leads
      .map(l => ({ lead: l, score: this.calcularScore(l) }))
      .sort((a, b) => b.score - a.score)
      .slice(0, 5);
  });

  ngOnInit() {
    this.cargarDashboard();
    this.cargarActividad();
    this.cargarLeadsHoy();
  }

  private cargarDashboard() {
    const id = this.authService.getIdAgente();
    if (!id) return;
    this.agenteService.getDashboard(id, this.periodoActivo()).subscribe({
      next: data => this.dashboard.set(data),
      error: () => this.dashboard.set(null),
    });
  }

  private cargarActividad() {
    const id = this.authService.getIdAgente();
    if (!id) return;
    this.agenteService.getActividadReciente(id).subscribe({
      next: data => this.actividadReciente.set(data),
      error: () => this.actividadReciente.set([]),
    });
  }

  private cargarLeadsHoy() {
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

  setPeriodo(p: DashboardPeriodo) {
    if (this.periodoActivo() === p) return;
    this.periodoActivo.set(p);
    this.cargarDashboard();
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

  deltaSimbolo(dir: 'up' | 'down' | 'flat' | undefined): string {
    if (dir === 'up') return '↑';
    if (dir === 'down') return '↓';
    return '·';
  }

  deltaClase(dir: 'up' | 'down' | 'flat' | undefined, invertido = false): string {
    if (!dir || dir === 'flat') return 'flat';
    // up = bueno por default; si invertido (tiempo de respuesta), up = malo
    if (invertido) return dir === 'up' ? 'down' : 'up';
    return dir;
  }

  formatDelta(pct: number | undefined): string {
    if (pct === undefined || pct === null) return '';
    const sign = pct > 0 ? '+' : '';
    return `${sign}${pct.toFixed(1)}%`;
  }

  // ---- helpers SVG ----

  private linePath(values: number[], w: number, h: number): string {
    if (values.length === 0) return '';
    const max = Math.max(1, ...values);
    return this.buildPath(values, w, h, max, false);
  }

  private areaPath(values: number[], w: number, h: number): string {
    if (values.length === 0) return '';
    const max = Math.max(1, ...values);
    return this.buildPath(values, w, h, max, true);
  }

  private linePathScaled(values: number[], w: number, h: number, maxY: number): string {
    if (values.length === 0) return '';
    return this.buildPath(values, w, h, maxY, false);
  }

  private areaPathScaled(values: number[], w: number, h: number, maxY: number): string {
    if (values.length === 0) return '';
    return this.buildPath(values, w, h, maxY, true);
  }

  private buildPath(values: number[], w: number, h: number, max: number, fill: boolean): string {
    const n = values.length;
    const step = n > 1 ? w / (n - 1) : 0;
    let d = '';
    for (let i = 0; i < n; i++) {
      const x = +(i * step).toFixed(2);
      const y = this.scaleY(values[i], max, h);
      d += (i === 0 ? `M ${x} ${y}` : ` L ${x} ${y}`);
    }
    if (fill) d += ` L ${w} ${h} L 0 ${h} Z`;
    return d;
  }

  private scaleY(v: number, max: number, h: number): number {
    const padTop = 4;
    const usable = h - padTop;
    if (max <= 0) return h;
    return +(h - (v / max) * usable).toFixed(2);
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

  private formatFechaCorta(iso: string | undefined): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('es-AR', { day: '2-digit', month: 'short' });
  }
}
