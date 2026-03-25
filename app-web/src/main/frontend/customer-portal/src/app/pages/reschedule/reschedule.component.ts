import { Component } from '@angular/core';

@Component({
  selector: 'app-reschedule',
  standalone: true,
  template: `
    <h2 class="fw-bold mb-4" style="color: #fd7e14;"><i class="bi bi-arrow-clockwise me-2"></i>Reschedule</h2>
    <div class="card">
      <div class="card-body">
        <p class="text-muted">Reschedule your appointments with context-aware assistance.</p>
      </div>
    </div>
  `
})
export class RescheduleComponent {}
