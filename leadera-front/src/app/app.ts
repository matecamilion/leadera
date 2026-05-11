import { Component, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Header } from './shared/header/header';
import { NgIf } from '@angular/common';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('leadera-front');

   mostrarHeader = true;
  private rutasOcultas = ['/login', '/register'];

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.mostrarHeader = !this.rutasOcultas.includes(event.urlAfterRedirects);
      }
    });
  }
}
