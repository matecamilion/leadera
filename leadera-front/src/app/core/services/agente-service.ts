import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ActividadReciente {
  tipoInteraccion: string;
  detalle: string;
  fechaInteraccion: string;
  leadNombre: string;
  leadApellido: string;
  leadId: number;
}

@Injectable({
  providedIn: 'root',
})
export class AgenteService {
   private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboardStats(agenteId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/leads/agente/${agenteId}/stats`);
  }

  getActividadReciente(agenteId: number): Observable<ActividadReciente[]> {
    return this.http.get<ActividadReciente[]>(`${this.apiUrl}/leads/agente/${agenteId}/actividad-reciente`);
  }
}
