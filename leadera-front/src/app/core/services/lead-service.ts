import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LeadsHoyResponse } from '../models/leads-hoy-response';
import { Lead } from '../models/lead';
import { LeadHoy } from '../models/lead-hoy';
import { LeadResumen } from '../models/lead-resumen';
import { CrearLeadRequest } from '../models/crear-lead-request';
import { Page } from '../models/page';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class LeadService {
  private apiUrl = `${environment.apiUrl}/leads`;

  constructor(private http: HttpClient) {}

  getLeads(page = 0, size = 20): Observable<Page<LeadResumen>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<Page<LeadResumen>>(`${this.apiUrl}`, { params });
  }

  getLeadsHoy(): Observable<LeadsHoyResponse> {
    return this.http.get<LeadsHoyResponse>(`${this.apiUrl}/hoy`);
  }

  getLeadById(id: number): Observable<Lead> {
    return this.http.get<Lead>(`${this.apiUrl}/${id}`);
  }

  editarContacto(id: number, nuevoTelefono: string, nuevoEmail: string): Observable<Lead> {
    return this.http.put<Lead>(`${this.apiUrl}/${id}/editar-contacto`, {
      telefono: nuevoTelefono,
      email: nuevoEmail
    });
  }

  actualizarEstado(id: number, nuevoEstado: string): Observable<Lead> {
    return this.http.put<Lead>(`${this.apiUrl}/${id}/estado`, null, {
      params: { nuevoEstado: nuevoEstado }
    });
  }

  establecerLeadInactivo(id: number): Observable<Lead> {
    return this.http.patch<Lead>(`${this.apiUrl}/${id}/estado`, {});
  }

  crearLead(lead: CrearLeadRequest): Observable<Lead> {
    return this.http.post<Lead>(this.apiUrl, lead);
  }

  eliminarLead(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getLeadsPorSeccion(
    seccion: string,
    nombre: string,
    tipoOperacion: string,
    page: number,
    size: number
  ): Observable<{ content: LeadHoy[]; totalElements: number; totalPages: number }> {
    let params = new HttpParams()
      .set('seccion', seccion)
      .set('nombre', nombre)
      .set('page', page)
      .set('size', size);
    if (tipoOperacion) {
      params = params.set('tipoOperacion', tipoOperacion);
    }
    return this.http.get<{ content: LeadHoy[]; totalElements: number; totalPages: number }>(
      `${this.apiUrl}/seccion`, { params }
    );
  }
}
