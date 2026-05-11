import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LeadsHoyResponse } from '../models/leads-hoy-response';
import { Lead } from '../models/lead';
import { LeadResumen } from '../models/lead-resumen';

@Injectable({
  providedIn: 'root',
})
export class LeadService {
  private apiUrl = 'http://localhost:8080/leads';

  constructor(private http: HttpClient) {}

  getLeads(): Observable<LeadResumen[]> {
    return this.http.get<LeadResumen[]>(`${this.apiUrl}`);
  }

  getLeadsHoy(): Observable<LeadsHoyResponse> {
    return this.http.get<LeadsHoyResponse>(`${this.apiUrl}/hoy`);
  }

  getLeadById(id: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/${id}`);
}

 editarContacto(id: number, nuevoTelefono: string, nuevoEmail: string): Observable<Lead> {
    return this.http.put<Lead>(`${this.apiUrl}/${id}/editar-contacto`, {
      telefono: nuevoTelefono,
      email: nuevoEmail
    });
  }

  actualizarEstado(id: number, nuevoEstado: string): Observable<Lead> {
    // Según tu Controller, la ruta es /leads/{id}/estado
    // y espera el parámetro 'nuevoEstado' en la URL.
    return this.http.put<Lead>(`${this.apiUrl}/${id}/estado`, null, {
      params: { nuevoEstado: nuevoEstado }
    });
  }

  establecerLeadInactivo(id: number): Observable<Lead> {
    
    return this.http.patch<Lead>(`${this.apiUrl}/${id}/estado`,{})
  }

  crearLead(lead: any): Observable<Lead> {
    return this.http.post<Lead>(this.apiUrl, lead);
  }
  

}
