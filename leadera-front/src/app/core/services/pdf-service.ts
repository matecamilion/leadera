import { Injectable } from '@angular/core';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { LeadResumen } from '../models/lead-resumen';

@Injectable({
  providedIn: 'root',
})
export class PdfService {
  exportarLeads(leads: LeadResumen[], agenteNombre: string): void {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'pt' });
    const margenX = 40;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.setTextColor(15, 23, 42);
    doc.text('LeadEra — Listado de Leads', margenX, 50);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.setTextColor(100, 116, 139);
    doc.text(`Agente: ${agenteNombre}`, margenX, 70);
    doc.text(`Generado: ${this.fechaCompleta()}`, margenX, 85);
    doc.text(`Total leads: ${leads.length}`, margenX, 100);

    autoTable(doc, {
      startY: 120,
      head: [[
        'Nombre',
        'Teléfono',
        'Email',
        'Estado',
        'Origen',
        'Ingreso',
        'Último contacto',
        'V',
        'C',
        'A',
        'Total',
      ]],
      body: leads.map((l) => {
        const total = l.operacionesVenta + l.operacionesCompra + l.operacionesAlquiler;
        return [
          `${l.nombre} ${l.apellido}`,
          l.telefono || '—',
          l.email || '—',
          l.estado,
          l.origen || '—',
          this.fechaCorta(l.fechaEntrada),
          this.fechaCorta(l.ultimoContacto),
          String(l.operacionesVenta),
          String(l.operacionesCompra),
          String(l.operacionesAlquiler),
          String(total),
        ];
      }),
      headStyles: {
        fillColor: [30, 41, 59],
        textColor: 255,
        fontStyle: 'bold',
        fontSize: 9,
      },
      bodyStyles: { fontSize: 8, textColor: [30, 41, 59] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: {
        7: { halign: 'center' },
        8: { halign: 'center' },
        9: { halign: 'center' },
        10: { halign: 'center', fontStyle: 'bold' },
      },
      didParseCell: (data) => {
        if (data.section === 'body' && data.column.index === 3) {
          const estado = String(data.cell.raw);
          data.cell.styles.textColor = this.colorPorEstado(estado);
          data.cell.styles.fontStyle = 'bold';
        }
      },
      margin: { left: margenX, right: margenX },
    });

    doc.save(`leadera-leads-${this.fechaArchivo()}.pdf`);
  }

  private fechaCorta(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  private fechaCompleta(): string {
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
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private colorPorEstado(estado: string): [number, number, number] {
    switch (estado) {
      case 'CALIENTE': return [220, 38, 38];
      case 'TIBIO':    return [217, 119, 6];
      case 'FRIO':     return [37, 99, 235];
      case 'GANADO':   return [22, 163, 74];
      case 'INACTIVO': return [100, 116, 139];
      default:         return [30, 41, 59];
    }
  }
}
