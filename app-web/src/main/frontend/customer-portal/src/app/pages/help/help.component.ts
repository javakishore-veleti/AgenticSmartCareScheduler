import { Component } from '@angular/core';

@Component({
  selector: 'app-help',
  standalone: true,
  template: `
    <h2 class="fw-bold mb-4" style="color: #0dcaf0;"><i class="bi bi-question-circle me-2"></i>Help</h2>
    <div class="card">
      <div class="card-body">
        <p class="text-muted">How SmartCare works to keep you connected with your healthcare provider.</p>
      </div>
    </div>
  `
})
export class HelpComponent {}
