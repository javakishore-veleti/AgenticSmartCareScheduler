import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AppointmentsComponent } from './pages/appointments/appointments.component';
import { RescheduleComponent } from './pages/reschedule/reschedule.component';
import { PreferencesComponent } from './pages/preferences/preferences.component';
import { HelpComponent } from './pages/help/help.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'appointments', component: AppointmentsComponent },
  { path: 'reschedule', component: RescheduleComponent },
  { path: 'preferences', component: PreferencesComponent },
  { path: 'help', component: HelpComponent },
  { path: '**', redirectTo: '' }
];
