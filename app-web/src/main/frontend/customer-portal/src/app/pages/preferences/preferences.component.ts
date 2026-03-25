import { Component } from '@angular/core';

@Component({
  selector: 'app-preferences',
  standalone: true,
  template: `
    <h2 class="fw-bold mb-4" style="color: #6f42c1;"><i class="bi bi-gear me-2"></i>Communication Preferences</h2>
    <div class="card">
      <div class="card-body">
        <p class="text-muted">Set your preferred outreach channel and contact times.</p>
      </div>
    </div>
  `
})
export class PreferencesComponent {}
