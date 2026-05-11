import { Lead } from "./lead";

export interface LeadsHoyResponse {
    nuevosSinContacto: Lead[];
    prioritarios: Lead[];
    seguimientosDeHoy: Lead[];
    contactadosHoy: Lead[];
    totalTareasDelDia: number;     
    tareasCompletadasDelDia: number;
}
