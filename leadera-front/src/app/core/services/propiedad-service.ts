import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Propiedad } from '../models/propiedad';
import { EventoPropiedad } from '../models/evento-propiedad';

@Injectable({ providedIn: 'root' })
export class PropiedadService {
  private http = inject(HttpClient);
  private base = 'http://localhost:8080/propiedades';

  obtenerPorLead(leadId: number): Observable<Propiedad[]> {
    return this.http.get<Propiedad[]>(`${this.base}/lead/${leadId}`);
  }

  agregar(leadId: number, propiedad: Partial<Propiedad>): Observable<Propiedad> {
    return this.http.post<Propiedad>(`${this.base}/lead/${leadId}`, propiedad);
  }

  actualizarEstado(id: number, estado: string): Observable<Propiedad> {
    return this.http.patch<Propiedad>(`${this.base}/${id}/estado`, { estado });
  }

  registrarEvento(propiedadId: number, evento: Partial<EventoPropiedad>): Observable<EventoPropiedad> {
    return this.http.post<EventoPropiedad>(`${this.base}/${propiedadId}/eventos`, evento);
  }

  obtenerEventos(propiedadId: number): Observable<EventoPropiedad[]> {
    return this.http.get<EventoPropiedad[]>(`${this.base}/${propiedadId}/eventos`);
  }

  obtenerPorId(id: number): Observable<Propiedad> {
  return this.http.get<Propiedad>(`${this.base}/${id}`);
}
}