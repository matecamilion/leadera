export interface AgenteDashboard {
  activos: number;
  calientes: number;
  tibios: number;
  frios: number;
  ganadosMes: number;
  nuevosDelMes: number;
  perdidos: number;
  interacciones7d: number;
  tasaConversion: number;
  tiempoRespuestaDias: number;
  // Nombre real del campo en AgenteDashboardDTO del backend (el de arriba
  // quedó por compatibilidad con vistas viejas; revisar en la auditoría).
  tiempoRespuesta?: number;
}
