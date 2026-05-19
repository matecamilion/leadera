import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoginRequest } from '../models/login-request';
import { RegistroRequest } from '../models/registro-request';
import { LoginResponse } from '../models/page';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;

  currentUserSig = signal<string | null>(localStorage.getItem('agente_nombre'));

  constructor(private http: HttpClient) {}

  registrar(agente: RegistroRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, agente);
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        const nombreCompleto = `${res.nombre} ${res.apellido}`;
        localStorage.setItem('token', res.token);
        localStorage.setItem('agente_nombre', nombreCompleto);
        localStorage.setItem('agente_email', res.email);

        this.currentUserSig.set(nombreCompleto);
      })
    );
  }

  getNombreAgente() { return this.currentUserSig(); }
  getEmailAgente() { return localStorage.getItem('agente_email'); }

  getIdAgente(): number | null {
    const token = localStorage.getItem('token');
    if (!token) return null;

    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.id;
  }

  logout(): void {
    localStorage.clear();
  }

  getToken() {
    return localStorage.getItem('token');
  }
}
