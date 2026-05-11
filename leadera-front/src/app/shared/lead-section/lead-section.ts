import { Component, input , output} from '@angular/core';
import { Lead } from '../../core/models/lead';
import { LeadCard } from '../lead-card/lead-card';

@Component({
  selector: 'app-lead-section',
  standalone: true,
  imports: [LeadCard],
  templateUrl: './lead-section.html',
  styleUrl: './lead-section.css',
})
export class LeadSection {
  title = input.required<string>();
  subtitle = input.required<string>();
  badgeText = input.required<string>();
  variant = input.required<'prioritarios' | 'seguimientos' | 'nuevos' | 'completados'>();
  leads = input.required<Lead[]>();
  leadsContactados = input<number[]>([]);
  tareaCompletada = output<number>();

  reenviarTareaCompletada(leadId: number) {
    this.tareaCompletada.emit(leadId);
  }
}