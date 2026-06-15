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
import { TareaService, TareasCumplimientoDTO } from '../../core/services/tarea-service';
import { LeadHoy } from '../../core/models/lead-hoy';

const DONUT_RADIUS = 56;

// Geometría del gráfico de evolución (viewBox 0 0 720 220).
const EVOL_W = 720;
const EVOL_PAD_LEFT = 30; // espacio para las etiquetas del eje Y
const EVOL_TOP = 12;
const EVOL_BASELINE = 204;

interface EvolBarra {
  x: number;
  y: number;
  width: number;
  height: number;
  color: string;
  key: 'nuevos' | 'ganados' | 'perdidos';
}
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
  private tareaService = inject(TareaService);
  private router = inject(Router);

  dashboard = signal<DashboardData | null>(null);
  leadsHoy = signal<LeadHoy[]>([]);
  actividadReciente = signal<ActividadReciente[]>([]);
  cumplimiento = signal<TareasCumplimientoDTO | null>(null);

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

  // Evolución (barras agrupadas por día). Con conteos diarios dispersos las
  // curvas suavizadas inflaban cada evento en una "campana" de varios días.
  private evolEscala = computed(() => {
    const ev = this.dashboard()?.evolucion;
    const realMax = ev
      ? Math.max(0, ...ev.nuevos, ...ev.ganados, ...ev.perdidos)
      : 0;
    // Piso de 3: un día con 1 lead no debe tocar el techo del gráfico.
    const maxY = Math.max(3, realMax);
    if (maxY <= 5) {
      return { max: maxY, ticks: Array.from({ length: maxY + 1 }, (_, i) => i) };
    }
    const step = Math.ceil(maxY / 4);
    return { max: step * 4, ticks: [0, step, step * 2, step * 3, step * 4] };
  });

  evolTicksY = computed(() =>
    this.evolEscala().ticks.map(valor => ({ valor, y: this.evolY(valor) }))
  );

  evolVacio = computed(() =>
    this.totalNuevosPeriodo() + this.totalGanadosPeriodo() + this.totalPerdidosPeriodo() === 0
  );

  evolBarras = computed<EvolBarra[]>(() => {
    const ev = this.dashboard()?.evolucion;
    if (!ev || ev.fechas.length === 0) return [];

    // Se itera el período completo: cada día conserva su posición real en el
    // eje X aunque no tenga eventos (los días en cero no emiten rects).
    const dias = ev.fechas.length;
    const groupW = (EVOL_W - EVOL_PAD_LEFT) / dias;
    const barW = Math.max(2, +((groupW * 0.7) / 3).toFixed(2));

    const series = [
      { key: 'nuevos' as const, valores: ev.nuevos, color: 'var(--brand)' },
      { key: 'ganados' as const, valores: ev.ganados, color: 'var(--brand-deep)' },
      { key: 'perdidos' as const, valores: ev.perdidos, color: 'var(--ink-4)' },
    ];

    const barras: EvolBarra[] = [];
    for (let i = 0; i < dias; i++) {
      const x0 = EVOL_PAD_LEFT + i * groupW + (groupW - barW * 3) / 2;
      series.forEach((s, si) => {
        const v = s.valores[i] ?? 0;
        if (v <= 0) return;
        const y = this.evolY(v);
        barras.push({
          x: +(x0 + si * barW).toFixed(2),
          y,
          width: barW,
          height: +(EVOL_BASELINE - y).toFixed(2),
          color: s.color,
          key: s.key,
        });
      });
    }
    return barras;
  });

  private evolY(valor: number): number {
    const { max } = this.evolEscala();
    return +(EVOL_BASELINE - (valor / max) * (EVOL_BASELINE - EVOL_TOP)).toFixed(2);
  }

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
    if (this.authService.esAsistente()) {
      this.router.navigate(['/mis-tareas'], { replaceUrl: true });
      return;
    }
    this.cargarDashboard();
    this.cargarActividad();
    this.cargarLeadsHoy();
    this.tareaService.obtenerCumplimientoMes().subscribe({
      next: (c) => this.cumplimiento.set(c),
      error: () => {},
    });
  }

  private cargarDashboard() {
    this.agenteService.getDashboard(this.periodoActivo()).subscribe({
      next: data => this.dashboard.set(data),
      error: () => this.dashboard.set(null),
    });
  }

  private cargarActividad() {
    this.agenteService.getActividadReciente().subscribe({
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

  calcularScore(lead: LeadHoy): number {
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

  getAccionLabel(lead: LeadHoy): string {
    if (!lead.ultimoContacto) return 'Contactar';
    const dias = (Date.now() - new Date(lead.ultimoContacto).getTime()) / 86400000;
    if (lead.estado === 'CALIENTE' && dias < 1) return 'Preparar';
    if (lead.estado === 'CALIENTE') return 'Enviar';
    if (lead.estado === 'TIBIO' && dias > 4) return 'Reactivar';
    return 'Llamar';
  }

  getSubtituloLead(lead: LeadHoy): string {
    if (!lead.ultimoContacto) return 'Nunca contactado';
    const dias = Math.floor((Date.now() - new Date(lead.ultimoContacto).getTime()) / 86400000);
    if (dias === 0) return 'Contactado hoy';
    if (dias === 1) return 'Hace 1 día';
    return `Hace ${dias} días`;
  }

  inicialesLead(lead: LeadHoy): string {
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

  private buildPath(values: number[], w: number, h: number, max: number, fill: boolean): string {
  const n = values.length;
  if (n === 0) return '';
  if (n === 1) {
    const x = w / 2;
    const y = this.scaleY(values[0], max, h);
    return fill ? `M 0 ${h} L 0 ${y} L ${w} ${y} L ${w} ${h} Z` : `M 0 ${y} L ${w} ${y}`;
  }

  const step = w / (n - 1);
  const points = values.map((v, i) => ({
    x: +(i * step).toFixed(2),
    y: this.scaleY(v, max, h),
  }));

  // Tensión de la curva (0.3 = suave pero fiel a los datos)
  const t = 0.3;

  let d = `M ${points[0].x} ${points[0].y}`;

  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1];
    const curr = points[i];

    const cp1x = +(prev.x + (curr.x - prev.x) * t).toFixed(2);
    const cp1y = +prev.y.toFixed(2);
    const cp2x = +(curr.x - (curr.x - prev.x) * t).toFixed(2);
    const cp2y = +curr.y.toFixed(2);

    d += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${curr.x} ${curr.y}`;
  }

  if (fill) {
    d += ` L ${points[n - 1].x} ${h} L ${points[0].x} ${h} Z`;
  }

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
