import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LeadService } from '../../core/services/lead-service';
import { Lead } from '../../core/models/lead';
import { LeadSection } from '../../shared/lead-section/lead-section';

@Component({
  selector: 'app-gestion-del-dia',
  standalone: true,
  imports: [CommonModule, RouterModule, LeadSection],
  templateUrl: './gestion-del-dia.html',
  styleUrl: './gestion-del-dia.css'
})
export class GestionDelDia implements OnInit {
  leads = signal<Lead[]>([]);
  cargando = signal(true);

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.cargarHistorial();
  }

  cargarHistorial(): void {
    this.cargando.set(true);
    this.leadService.getLeadsHoy().subscribe({
      next: (res) => {
        // Solo nos interesan los que ya fueron contactados
        this.leads.set(res.contactadosHoy || []);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }
}