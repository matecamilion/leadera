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
  private rutasOcultas = ['/login', '/register'];

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.mostrarSidebar = !this.rutasOcultas.includes(event.urlAfterRedirects);
      }
    });
  }
}
