import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoginRequest } from '../models/login-request';
import { RegistroRequest } from '../models/registro-request';
import { LoginResponse } from '../models/page';

export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}

export type RolAgente = 'DUENO' | 'AGENTE' | 'ASISTENTE';

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
        // Flag del flujo de password temporal: mientras esté en true, el
        // authGuard bloquea toda la navegación salvo /cambiar-password.
        localStorage.setItem('debe_cambiar_password', String(res.debeCambiarPassword));

        this.currentUserSig.set(nombreCompleto);
      })
    );
  }

  cambiarPassword(request: CambiarPasswordRequest): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/cambiar-password`, request).pipe(
      tap(() => localStorage.setItem('debe_cambiar_password', 'false'))
    );
  }

  debeCambiarPassword(): boolean {
    return localStorage.getItem('debe_cambiar_password') === 'true';
  }

  getNombreAgente() { return this.currentUserSig(); }
  getEmailAgente() { return localStorage.getItem('agente_email'); }

  getIdAgente(): number | null {
    const payload = this.getTokenPayload();
    return payload?.id ?? null;
  }

  // Rol leído del claim del JWT (fuente de verdad firmada por el backend).
  // Tokens emitidos antes del multi-tenant no traen el claim: se asume AGENTE.
  getRol(): RolAgente {
    const payload = this.getTokenPayload();
    if (payload?.rol === 'DUENO') return 'DUENO';
    if (payload?.rol === 'ASISTENTE') return 'ASISTENTE';
    return 'AGENTE';
  }

  esDueno(): boolean {
    return this.getRol() === 'DUENO';
  }

  esAsistente(): boolean {
    return this.getRol() === 'ASISTENTE';
  }

  logout(): void {
    localStorage.clear();
  }

  getToken() {
    return localStorage.getItem('token');
  }

  private getTokenPayload(): any | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch {
      return null;
    }
  }
}
