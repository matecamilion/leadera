import { Component, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Sidebar } from './shared/sidebar/sidebar';
import { ToastContainer } from './shared/toast-container/toast-container';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, ToastContainer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('leadera-front');

  mostrarSidebar = true;
  // cambiar-password sin sidebar: durante el cambio forzado de password
  // temporal el resto de la navegación está bloqueada por el authGuard.
  private rutasOcultas = ['/login', '/register', '/cambiar-password'];

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.mostrarSidebar = !this.rutasOcultas.includes(event.urlAfterRedirects);
      }
    });
  }
}
