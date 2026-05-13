import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Propiedad } from '../models/propiedad';
import { environment } from '../../../environments/environment';

export interface PropiedadResumen {
  id: number;
  titulo: string;
  tipo: string;
  estado: 'DISPONIBLE' | 'RESERVADA' | 'VENDIDA';
  precio: number | null;
  leadId: number;
  leadNombre: string;
}

@Injectable({ providedIn: 'root' })
export class PropiedadService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/propiedades`;

  obtenerTodas(): Observable<PropiedadResumen[]> {
    return this.http.get<PropiedadResumen[]>(this.base);
  }

  obtenerPorLead(leadId: number): Observable<Propiedad[]> {
    return this.http.get<Propiedad[]>(`${this.base}/lead/${leadId}`);
  }

  agregar(leadId: number, propiedad: Partial<Propiedad>): Observable<Propiedad> {
    return this.http.post<Propiedad>(`${this.base}/lead/${leadId}`, propiedad);
  }

  actualizarEstado(id: number, estado: string): Observable<Propiedad> {
    return this.http.patch<Propiedad>(`${this.base}/${id}/estado`, { estado });
  }

  editar(id: number, body: Partial<Propiedad>): Observable<Propiedad> {
    return this.http.put<Propiedad>(`${this.base}/${id}`, body);
  }

  obtenerPorId(id: number): Observable<Propiedad> {
  return this.http.get<Propiedad>(`${this.base}/${id}`);
}
}