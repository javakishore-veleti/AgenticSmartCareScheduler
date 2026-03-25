import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="mb-4">
      <h2 class="fw-bold" style="color: #667eea;">
        <i class="bi bi-sunrise me-2"></i>Welcome back, Patient
      </h2>
      <p class="text-muted">Your appointment coordination is powered by AI</p>
    </div>

    <!-- Status Cards -->
    <div class="row g-4 mb-4">
      <div class="col-md-3">
        <div class="card h-100" style="border-left: 4px solid #198754;">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <p class="text-muted small mb-1">Upcoming</p>
                <h3 class="fw-bold" style="color: #198754;">2</h3>
              </div>
              <i class="bi bi-calendar-check fs-2" style="color: #198754; opacity: 0.3;"></i>
            </div>
            <span class="badge rounded-pill" style="background: #19875422; color: #198754;">Next: Tomorrow 2 PM</span>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card h-100" style="border-left: 4px solid #667eea;">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <p class="text-muted small mb-1">Confirmed</p>
                <h3 class="fw-bold" style="color: #667eea;">5</h3>
              </div>
              <i class="bi bi-check-circle fs-2" style="color: #667eea; opacity: 0.3;"></i>
            </div>
            <span class="badge rounded-pill" style="background: #667eea22; color: #667eea;">This month</span>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card h-100" style="border-left: 4px solid #fd7e14;">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <p class="text-muted small mb-1">Rescheduled</p>
                <h3 class="fw-bold" style="color: #fd7e14;">1</h3>
              </div>
              <i class="bi bi-arrow-repeat fs-2" style="color: #fd7e14; opacity: 0.3;"></i>
            </div>
            <span class="badge rounded-pill" style="background: #fd7e1422; color: #fd7e14;">Via Voice IVR</span>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card h-100" style="border-left: 4px solid #6f42c1;">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <p class="text-muted small mb-1">Preference</p>
                <h3 class="fw-bold" style="color: #6f42c1;"><i class="bi bi-phone-vibrate"></i></h3>
              </div>
              <i class="bi bi-sliders fs-2" style="color: #6f42c1; opacity: 0.3;"></i>
            </div>
            <span class="badge rounded-pill" style="background: #6f42c122; color: #6f42c1;">Voice IVR preferred</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Context-Aware Communication -->
    <div class="row g-4 mb-4">
      <div class="col-md-8">
        <div class="card">
          <div class="card-header bg-white border-0 pt-3">
            <h5 class="fw-bold"><i class="bi bi-broadcast me-2" style="color: #667eea;"></i>Smart Outreach History</h5>
          </div>
          <div class="card-body">
            <div class="table-responsive">
              <table class="table table-hover align-middle">
                <thead>
                  <tr style="color: #667eea;">
                    <th>Date</th>
                    <th>Appointment</th>
                    <th>Your Context</th>
                    <th>Channel Used</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Mar 24</td>
                    <td>Dr. Smith — 2:00 PM</td>
                    <td><span class="badge" style="background: #fd7e1422; color: #fd7e14;">In Transit</span></td>
                    <td><i class="bi bi-telephone-fill me-1" style="color: #198754;"></i>Voice IVR</td>
                    <td><span class="badge" style="background: #19875422; color: #198754;">Confirmed</span></td>
                  </tr>
                  <tr>
                    <td>Mar 20</td>
                    <td>Dr. Patel — 10:30 AM</td>
                    <td><span class="badge" style="background: #667eea22; color: #667eea;">Stationary</span></td>
                    <td><i class="bi bi-chat-dots-fill me-1" style="color: #0d6efd;"></i>SMS Deep-Link</td>
                    <td><span class="badge" style="background: #19875422; color: #198754;">Confirmed</span></td>
                  </tr>
                  <tr>
                    <td>Mar 15</td>
                    <td>Dr. Johnson — 4:30 PM</td>
                    <td><span class="badge" style="background: #dc354522; color: #dc3545;">Unreachable</span></td>
                    <td><i class="bi bi-telephone-inbound me-1" style="color: #6f42c1;"></i>Callback</td>
                    <td><span class="badge" style="background: #fd7e1422; color: #fd7e14;">Rescheduled</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
          <div class="card-body text-white">
            <h5 class="fw-bold"><i class="bi bi-robot me-2"></i>AI Context Agent</h5>
            <hr style="border-color: rgba(255,255,255,0.3);">
            <p class="small">Your SmartCare AI agent detects your context and reaches you through the best channel:</p>
            <div class="d-flex align-items-center mb-2">
              <i class="bi bi-car-front-fill me-2 fs-5"></i>
              <span class="small">In transit → Voice IVR (single keypress)</span>
            </div>
            <div class="d-flex align-items-center mb-2">
              <i class="bi bi-house-fill me-2 fs-5"></i>
              <span class="small">At home → SMS deep-link (one tap)</span>
            </div>
            <div class="d-flex align-items-center">
              <i class="bi bi-moon-fill me-2 fs-5"></i>
              <span class="small">Busy → Scheduled callback</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent {}
