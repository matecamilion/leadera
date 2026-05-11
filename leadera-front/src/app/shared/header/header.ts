import { Component, inject, signal } from '@angular/core';
import { RouterLink, Router } from "@angular/router";
import { AuthService } from '../../core/services/auth-service';
import { computed } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  private authService = inject(AuthService);
  private router = inject(Router);
  
  menuOpen = signal(false);

  // Ahora estas propiedades son reactivas
  agenteNombre = computed(() => this.authService.getNombreAgente() || 'Usuario');
  agenteEmail = computed(() => this.authService.getEmailAgente() || '');

  iniciales = computed(() => {
    const nombre = this.agenteNombre();
    if (!nombre || nombre === 'Usuario') return 'U';
    return nombre.split(' ').filter(n => n.length > 0).map(n => n[0]).join('').toUpperCase().substring(0, 2);
  });

  toggleMenu() {
    this.menuOpen.update(v => !v);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.menuOpen.set(false); // Cerramos el menú por las dudas
  }
}
