import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import { LeadResumen } from '../models/lead-resumen';

@Injectable({
  providedIn: 'root',
})
export class ExcelService {
  exportarLeads(leads: LeadResumen[], _agenteNombre: string): void {
    const filas = leads.map((l) => ({
      Nombre: l.nombre,
      Apellido: l.apellido,
      Teléfono: l.telefono ?? '',
      Email: l.email ?? '',
      Estado: l.estado,
      Origen: l.origen ?? '',
      'Fecha de ingreso': this.fechaCorta(l.fechaEntrada),
      'Fecha de último contacto': this.fechaCorta(l.ultimoContacto),
      'Operaciones VENTA': l.operacionesVenta,
      'Operaciones COMPRA': l.operacionesCompra,
      'Operaciones ALQUILER': l.operacionesAlquiler,
      'Total operaciones':
        l.operacionesVenta + l.operacionesCompra + l.operacionesAlquiler,
    }));

    const ws = XLSX.utils.json_to_sheet(filas);

    // Anchos de columna para que el archivo se vea legible al abrirlo.
    ws['!cols'] = [
      { wch: 18 }, // Nombre
      { wch: 18 }, // Apellido
      { wch: 16 }, // Teléfono
      { wch: 28 }, // Email
      { wch: 12 }, // Estado
      { wch: 18 }, // Origen
      { wch: 16 }, // Fecha de ingreso
      { wch: 22 }, // Fecha de último contacto
      { wch: 18 }, // Operaciones VENTA
      { wch: 18 }, // Operaciones COMPRA
      { wch: 20 }, // Operaciones ALQUILER
      { wch: 18 }, // Total operaciones
    ];

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Leads');
    XLSX.writeFile(wb, `leadera-leads-${this.fechaArchivo()}.xlsx`);
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

  private fechaArchivo(): string {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
