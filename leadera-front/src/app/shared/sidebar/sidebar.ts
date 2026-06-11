import { Component, inject, signal, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/services/auth-service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private authService = inject(AuthService);
  private router = inject(Router);

  sidebarOpen = signal(false);
  menuOpen = signal(false);

  agenteNombre = computed(() => this.authService.getNombreAgente() || 'Usuario');

  // Getter (no computed): el rol sale del token en localStorage, que no es
  // reactivo; el getter se re-evalúa en cada ciclo de change detection.
  get esDueno(): boolean {
    return this.authService.esDueno();
  }

  get esAsistente(): boolean {
    return this.authService.esAsistente();
  }

  iniciales = computed(() => {
    const nombre = this.agenteNombre();
    if (!nombre || nombre === 'Usuario') return 'U';
    return nombre
      .split(' ')
      .filter(n => n.length > 0)
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  });

  constructor() {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.sidebarOpen.set(false);
        this.menuOpen.set(false);
      }
    });
  }

  toggleSidebar() {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
    this.menuOpen.set(false);
  }

  toggleMenu() {
    this.menuOpen.update(v => !v);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.menuOpen.set(false);
    this.sidebarOpen.set(false);
  }
}
