import { Component, inject, computed } from '@angular/core';
import { AuthService } from '../../core/services/auth-service';
import { AgenteService } from '../../core/services/agente-service';


@Component({
  selector: 'app-perfil',
  imports: [],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {

  agenteNombre = computed(() => this.authService.getNombreAgente() || 'Usuario');
  agenteEmail = computed(() => this.authService.getEmailAgente() || '');

  mesActual = new Date().toLocaleString('es-AR', { month: 'long' });
  
  stats: any = null;

constructor(private agenteService: AgenteService, private authService: AuthService) {}

ngOnInit() {
  const id = this.authService.getIdAgente();
  console.log('ID del agente:', id); // ← agregá esto
  if (id) {
    this.agenteService.getDashboardStats(id).subscribe(data => {
      this.stats = data;
    });
  }
}
}
