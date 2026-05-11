import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AgenteService {
   private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getDashboardStats(agenteId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/leads/agente/${agenteId}/stats`);
  }
  
}
