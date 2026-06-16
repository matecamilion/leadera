import { Routes } from '@angular/router';
import { NuevaInteraccion } from './pages/nueva-interaccion/nueva-interaccion';
import { Home } from './pages/home/home';
import { ListadoLeadsComponent } from './pages/listar-leads/listar-leads';
import { DetalleLead } from './pages/detalle-lead/detalle-lead';
import { GestionDelDia } from './pages/gestion-del-dia/gestion-del-dia';
import { NuevoLead } from './pages/nuevo-lead/nuevo-lead';
import { Login } from './pages/auth/login/login';
import { Register } from './pages/auth/register/register';
import { Perfil } from './pages/perfil/perfil';
import { MiPerfil } from './pages/mi-perfil/mi-perfil';
import { authGuard } from './core/guards/auth-guard';
import { duenoGuard } from './core/guards/dueno-guard';
import { CambiarPassword } from './pages/auth/cambiar-password/cambiar-password';
import { Equipo } from './pages/equipo/equipo';
import { MiAsistente } from './pages/mi-asistente/mi-asistente';
import { MiEquipoAgente } from './pages/mi-equipo-agente/mi-equipo-agente';
import { MisTareas } from './pages/mis-tareas/mis-tareas';
import { PropiedadDetalle } from './pages/propiedad-detalle/propiedad-detalle';
import { DetalleOperacion } from './pages/detalle-operacion/detalle-operacion';
import { PropiedadesLead } from './pages/propiedades-lead/propiedades-lead';
import { KanbanBoardComponent } from './pages/kanban-board/kanban-board';
import { PropiedadesComponent } from './pages/propiedades/propiedades';
import { OperacionesLead } from './pages/operaciones-lead/operaciones-lead';
import { InteraccionesLead } from './pages/interacciones-lead/interacciones-lead';
import { OperacionesCerradasComponent } from './pages/operaciones-cerradas/operaciones-cerradas';
import { OperacionesPorEstado } from './pages/operaciones-por-estado/operaciones-por-estado';
import { LeadsSeccion } from './pages/leads-seccion/leads-seccion';

export const routes: Routes = [
    // 1. Rutas de Autenticación (Públicas)
   { path: 'login', component: Login },
  { path: 'register', component: Register },

  { path: 'cambiar-password', component: CambiarPassword, canActivate: [authGuard], title: 'LeadEra - Cambiar contraseña' },
  { path: 'equipo', component: Equipo, canActivate: [authGuard, duenoGuard], title: 'LeadEra - Mi equipo' },
  // Obsoleta (Fase 3): la reemplaza /mi-equipo-agente; queda ruteada pero sin link.
  { path: 'mi-asistente', component: MiAsistente, canActivate: [authGuard], title: 'LeadEra - Mi Asistente' },
  { path: 'mi-equipo-agente', component: MiEquipoAgente, canActivate: [authGuard], title: 'LeadEra - Mi Equipo' },
  { path: 'mis-tareas', component: MisTareas, canActivate: [authGuard], title: 'LeadEra - Mis Tareas' },

  { path: 'home', component: Home, canActivate: [authGuard] },
  { path: 'leads', component: ListadoLeadsComponent, canActivate: [authGuard] },
  { path: 'leads/nuevo', component: NuevoLead, canActivate: [authGuard] },
  { path: 'leads/seccion/:seccion', component: LeadsSeccion, canActivate: [authGuard], title: 'LeadEra - Sección' },
  { path: 'leads/:id', component: DetalleLead, canActivate: [authGuard] },
  { path: 'gestion-del-dia', component: GestionDelDia, canActivate: [authGuard] },
  { path: 'leads/:id/interaccion', component: NuevaInteraccion, canActivate: [authGuard] },
  { path: 'perfil', component: Perfil, canActivate: [authGuard] },
  { path: 'mi-perfil', component: MiPerfil, canActivate: [authGuard], title: 'LeadEra - Mi Perfil' },
  { path: 'propiedades/:id', component: PropiedadDetalle, canActivate: [authGuard] },
  { path: 'leads/:leadId/operaciones/:operacionId', component: DetalleOperacion, canActivate: [authGuard], title: 'LeadEra - Detalle Operación' },
  { path: 'leads/:id/propiedades', component: PropiedadesLead, canActivate: [authGuard], title: 'LeadEra - Propiedades del Lead' },
  { path: 'leads/:id/operaciones', component: OperacionesLead, canActivate: [authGuard], title: 'LeadEra - Operaciones del Lead' },
  { path: 'leads/:id/interacciones', component: InteraccionesLead, canActivate: [authGuard], title: 'LeadEra - Interacciones del Lead' },
  { path: 'pipeline', component: KanbanBoardComponent, canActivate: [authGuard], title: 'LeadEra - Pipeline' },
  { path: 'operaciones', component: KanbanBoardComponent, canActivate: [authGuard], title: 'LeadEra - Operaciones' },
  { path: 'operaciones/cerradas', component: OperacionesCerradasComponent, canActivate: [authGuard], title: 'LeadEra - Operaciones cerradas' },
  { path: 'operaciones/estado/:estado', component: OperacionesPorEstado, canActivate: [authGuard], title: 'LeadEra - Operaciones por estado' },
  { path: 'propiedades', component: PropiedadesComponent, canActivate: [authGuard], title: 'LeadEra - Propiedades' },

  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
  

];
