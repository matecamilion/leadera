import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MatchDelDia {
  propiedadId: number;
  direccion: string;
  zona: string;
  tipoVivienda: string;
  compradores: CompradorPotencial[];
}

// Espejo de CompradorPotencialDTO. El matching es compartido a nivel
// inmobiliaria: cuando esMio es false el lead pertenece a otro agente del
// equipo y el backend NUNCA envía su teléfono/email.
export interface CompradorPotencial {
  leadId: number;
  nombreLead: string;
  apellidoLead: string;
  estadoLead: string;
  operacionId: number;
  busquedaZona: string;
  busquedaTipoVivienda: string;
  busquedaPrecioMin: number;
  busquedaPrecioMax: number;
  busquedaAmbientes: number;
  busquedaMetros: number;
  agenteId: number;
  nombreAgente: string;
  apellidoAgente: string;
  esMio: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class MatchingService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/propiedades`;

  obtenerCompradores(propiedadId: number): Observable<CompradorPotencial[]> {
    return this.http.get<CompradorPotencial[]>(
      `${this.apiUrl}/${propiedadId}/compradores-potenciales`,
    );
  }

  obtenerMisMatches(): Observable<MatchDelDia[]> {
    return this.http.get<MatchDelDia[]>(
      `${environment.apiUrl}/propiedades/mis-matches`,
    );
  }
}
