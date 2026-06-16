import { LeadHoy } from "./lead-hoy";

export interface LeadsHoyResponse {
    nuevosSinContacto: LeadHoy[];
    prioritarios: LeadHoy[];
    seguimientosDeHoy: LeadHoy[];
    contactadosHoy: LeadHoy[];
    totalTareasDelDia: number;
    tareasCompletadasDelDia: number;
    totalPrioritarios: number;
    totalNuevos: number;
    totalSeguimientos: number;
}
