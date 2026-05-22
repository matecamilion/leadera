import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx-js-style';
import { LeadResumen } from '../models/lead-resumen';

type Alineacion = 'left' | 'center';

@Injectable({
  providedIn: 'root',
})
export class ExcelService {
  private readonly columnas = [
    'Nombre', 'Apellido', 'Teléfono', 'Email', 'Estado', 'Origen',
    'Fecha de ingreso', 'Último contacto',
    'Op. Venta', 'Op. Compra', 'Op. Alquiler', 'Total op.',
  ];

  // Alineación por columna (índice). Numéricas y estado centradas; el resto a la izquierda.
  private readonly alineacionPorCol: Alineacion[] = [
    'left', 'left', 'left', 'left', 'center', 'left',
    'left', 'left', 'center', 'center', 'center', 'center',
  ];

  private readonly anchosPorCol = [18, 18, 15, 25, 12, 15, 16, 16, 10, 10, 12, 10];

  exportarLeads(leads: LeadResumen[], agenteNombre: string): void {
    const numCols = this.columnas.length;
    const ultimaCol = numCols - 1;

    // === Armado del array of arrays (filas) ===
    const aoa: (string | number)[][] = [];

    // Fila 1: título (lo merged ocupa A1:L1)
    aoa.push(['LeadEra — Listado de Leads', ...this.vacios(numCols - 1)]);

    // Fila 2: metadata (agente en A2, exportado en G2)
    const fila2 = this.vacios(numCols);
    fila2[0] = `Agente: ${agenteNombre}`;
    fila2[6] = `Exportado el: ${this.fechaHoraCompleta()}`;
    aoa.push(fila2);

    // Fila 3: vacía
    aoa.push(this.vacios(numCols));

    // Fila 4: encabezados
    aoa.push([...this.columnas]);

    // Filas 5+: datos
    leads.forEach((l) => {
      aoa.push([
        l.nombre,
        l.apellido,
        l.telefono ?? '',
        l.email ?? '',
        l.estado,
        l.origen ?? '',
        this.fechaCorta(l.fechaEntrada),
        this.fechaCorta(l.ultimoContacto),
        l.operacionesVenta,
        l.operacionesCompra,
        l.operacionesAlquiler,
        l.operacionesVenta + l.operacionesCompra + l.operacionesAlquiler,
      ]);
    });

    const ws = XLSX.utils.aoa_to_sheet(aoa);

    // === Estilos ===
    this.aplicarEstilosTitulo(ws, ultimaCol);
    this.aplicarEstilosMetadata(ws, numCols);
    this.aplicarEstilosEncabezado(ws, numCols);
    this.aplicarEstilosDatos(ws, leads, numCols);

    // === Layout ===
    ws['!merges'] = [{ s: { r: 0, c: 0 }, e: { r: 0, c: ultimaCol } }];
    ws['!cols'] = this.anchosPorCol.map((wch) => ({ wch }));
    ws['!rows'] = [{ hpx: 30 }, { hpx: 18 }, undefined, { hpx: 22 }] as XLSX.RowInfo[];

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Leads');
    XLSX.writeFile(wb, `leadera-leads-${this.fechaArchivo()}.xlsx`);
  }

  // ---------- helpers de estilo ----------

  private aplicarEstilosTitulo(ws: XLSX.WorkSheet, ultimaCol: number): void {
    const estilo = {
      font: { bold: true, sz: 14, color: { rgb: 'FFFFFFFF' } },
      fill: { fgColor: { rgb: 'FF1E293B' } },
      alignment: { horizontal: 'center', vertical: 'center' },
    };
    // Aplicar al rango entero del merge para que el fondo no se vea cortado en algunos viewers.
    for (let c = 0; c <= ultimaCol; c++) {
      this.setStyle(ws, 0, c, estilo);
    }
  }

  private aplicarEstilosMetadata(ws: XLSX.WorkSheet, numCols: number): void {
    const estilo = {
      font: { italic: true, sz: 10, color: { rgb: 'FF475569' } },
      fill: { fgColor: { rgb: 'FFF1F5F9' } },
      alignment: { vertical: 'center' },
    };
    for (let c = 0; c < numCols; c++) {
      this.setStyle(ws, 1, c, estilo);
    }
  }

  private aplicarEstilosEncabezado(ws: XLSX.WorkSheet, numCols: number): void {
    const estilo = {
      font: { bold: true, sz: 11, color: { rgb: 'FFFFFFFF' } },
      fill: { fgColor: { rgb: 'FF1E293B' } },
      alignment: { horizontal: 'center', vertical: 'center' },
    };
    for (let c = 0; c < numCols; c++) {
      this.setStyle(ws, 3, c, estilo);
    }
  }

  private aplicarEstilosDatos(
    ws: XLSX.WorkSheet,
    leads: LeadResumen[],
    numCols: number,
  ): void {
    leads.forEach((lead, i) => {
      const row = 4 + i; // fila 5 (índice 4) en adelante
      const fondo = i % 2 === 0 ? 'FFFFFFFF' : 'FFF8FAFC';

      for (let c = 0; c < numCols; c++) {
        const esEstado = c === 4;
        const estilo: Record<string, any> = {
          font: { sz: 10, color: { rgb: 'FF1E293B' } },
          fill: { fgColor: { rgb: fondo } },
          alignment: { horizontal: this.alineacionPorCol[c], vertical: 'center' },
        };

        if (esEstado) {
          const { color, bold } = this.estiloEstado(lead.estado);
          estilo['font'] = { sz: 10, color: { rgb: color }, bold };
        }

        this.setStyle(ws, row, c, estilo);
      }
    });
  }

  private estiloEstado(estado: string): { color: string; bold: boolean } {
    switch (estado) {
      case 'CALIENTE': return { color: 'FFDC2626', bold: true };
      case 'TIBIO':    return { color: 'FFD97706', bold: true };
      case 'FRIO':     return { color: 'FF2563EB', bold: true };
      case 'GANADO':   return { color: 'FF15803D', bold: true };
      case 'INACTIVO': return { color: 'FF94A3B8', bold: false };
      default:         return { color: 'FF1E293B', bold: false };
    }
  }

  private setStyle(ws: XLSX.WorkSheet, r: number, c: number, style: any): void {
    const addr = XLSX.utils.encode_cell({ r, c });
    if (!ws[addr]) ws[addr] = { t: 's', v: '' };
    ws[addr].s = style;
  }

  // ---------- helpers de formato ----------

  private vacios(n: number): string[] {
    return Array.from({ length: n }, () => '');
  }

  private fechaCorta(iso: string | null | undefined): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleDateString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  private fechaHoraCompleta(): string {
    return new Date().toLocaleString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private fechaArchivo(): string {
    const d = new Date();
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}-${mm}-${yyyy}`;
  }
}
