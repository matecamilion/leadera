import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AgenteService {
   private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboardStats(agenteId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/leads/agente/${agenteId}/stats`);
  }
  
}
