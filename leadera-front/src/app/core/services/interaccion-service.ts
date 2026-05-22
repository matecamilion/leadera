import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Interaccion } from '../models/interaccion';

export interface CrearInteraccionRequest {
  detalle: string;
  tipoInteraccion: string;
  // ISO local sin zona (YYYY-MM-DDTHH:mm[:ss]). Jackson lo parsea a LocalDateTime.
  proximoContacto?: string;
}

@Injectable({
  providedIn: 'root',
})
export class InteraccionService {
  private apiUrl = `${environment.apiUrl}/leads`;

  constructor(private http: HttpClient) {}

  obtenerInteraccionesDelLead(leadId: number): Observable<Interaccion[]> {
    return this.http.get<Interaccion[]>(`${this.apiUrl}/${leadId}/interacciones`);
  }

  crearInteraccion(leadId: number, body: CrearInteraccionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${leadId}/interacciones`, body);
  }
}
