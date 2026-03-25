import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AgentsComponent } from './pages/agents/agents.component';
import { ScenariosComponent } from './pages/scenarios/scenarios.component';
import { AnalyticsComponent } from './pages/analytics/analytics.component';
import { DatasetsComponent } from './pages/datasets/datasets.component';
import { DigitalTwinComponent } from './pages/digital-twin/digital-twin.component';
import { AuditComponent } from './pages/audit/audit.component';
import { SettingsComponent } from './pages/settings/settings.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'agents', component: AgentsComponent },
  { path: 'scenarios', component: ScenariosComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'datasets', component: DatasetsComponent },
  { path: 'digital-twin', component: DigitalTwinComponent },
  { path: 'audit', component: AuditComponent },
  { path: 'settings', component: SettingsComponent },
  { path: '**', redirectTo: '' }
];
