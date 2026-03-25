import { Component } from '@angular/core';

@Component({
  selector: 'app-appointments',
  standalone: true,
  template: `
    <h2 class="fw-bold mb-4" style="color: #198754;"><i class="bi bi-calendar2-week me-2"></i>My Appointments</h2>
    <div class="card">
      <div class="card-body">
        <p class="text-muted">Your upcoming and past appointments will appear here.</p>
      </div>
    </div>
  `
})
export class AppointmentsComponent {}
