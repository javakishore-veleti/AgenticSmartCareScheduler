import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- Setup Alert -->
    <div *ngIf="setupStatus && !setupStatus.allSeeded" class="alert mb-4" style="background: linear-gradient(135deg, #fef3c7, #fde68a); border: 1px solid #d97706; border-radius: 12px;">
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <h5 class="fw-bold mb-1" style="color: #92400e;">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>Foundational Data Setup Required
          </h5>
          <p class="mb-2 small" style="color: #78350f;">
            {{ setupStatus.pendingSetup.length }} setup step(s) pending. Seed foundational data to enable all features.
          </p>
          <div class="d-flex flex-wrap gap-2">
            <span *ngFor="let p of setupStatus.pendingSetup" class="badge" style="background: #fef9c3; color: #854d0e; border: 1px solid #d97706;">
              <i class="bi bi-circle me-1"></i>{{ p.label }}
            </span>
          </div>
        </div>
        <div class="d-flex gap-2">
          <a routerLink="/system-setup" class="btn btn-sm" style="background: #d97706; color: white; border-radius: 8px;">
            <i class="bi bi-gear me-1"></i>View Details
          </a>
          <button class="btn btn-sm" style="background: #92400e; color: white; border-radius: 8px;"
                  (click)="seedAll()" [disabled]="seeding">
            <i class="bi bi-cloud-download me-1"></i>{{ seeding ? 'Seeding...' : 'Seed All Now' }}
          </button>
        </div>
      </div>
    </div>

    <h2 class="fw-bold mb-4" style="color: #0d9488;"><i class="bi bi-speedometer2 me-2"></i>Admin Dashboard</h2>
    <div class="row g-4 mb-4">
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #4f46e5;">
          <div class="card-body"><i class="bi bi-people fs-2" style="color:#4f46e5;"></i><h4 class="fw-bold mt-2">1,247</h4><p class="text-muted small mb-0">Today's Appointments</p></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #16a34a;">
          <div class="card-body"><i class="bi bi-check-circle fs-2" style="color:#16a34a;"></i><h4 class="fw-bold mt-2">89%</h4><p class="text-muted small mb-0">Confirmed</p></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #ea580c;">
          <div class="card-body"><i class="bi bi-exclamation-triangle fs-2" style="color:#ea580c;"></i><h4 class="fw-bold mt-2">43</h4><p class="text-muted small mb-0">At-Risk Slots</p></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #0d9488;">
          <div class="card-body"><i class="bi bi-arrow-repeat fs-2" style="color:#0d9488;"></i><h4 class="fw-bold mt-2">28</h4><p class="text-muted small mb-0">Reallocated</p></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #e11d48;">
          <div class="card-body"><i class="bi bi-cpu fs-2" style="color:#e11d48;"></i><h4 class="fw-bold mt-2">5/5</h4><p class="text-muted small mb-0">Agents Active</p></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="card text-center" style="border-top: 3px solid #7c3aed;">
          <div class="card-body"><i class="bi bi-lightning fs-2" style="color:#7c3aed;"></i><h4 class="fw-bold mt-2">2.1s</h4><p class="text-muted small mb-0">Avg Latency</p></div>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <div class="col-md-6">
        <div class="card">
          <div class="card-header bg-white border-0 pt-3"><h5 class="fw-bold"><i class="bi bi-broadcast me-2" style="color:#0d9488;"></i>Agent Pipeline Status</h5></div>
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <span class="badge rounded-pill fs-6" style="background:#4f46e522; color:#4f46e5;">PCA</span>
              <div class="progress flex-grow-1 mx-2" style="height:8px;"><div class="progress-bar" style="width:100%; background:#4f46e5;"></div></div>
              <span class="badge rounded-pill fs-6" style="background:#ea580c22; color:#ea580c;">COA</span>
              <div class="progress flex-grow-1 mx-2" style="height:8px;"><div class="progress-bar" style="width:100%; background:#ea580c;"></div></div>
              <span class="badge rounded-pill fs-6" style="background:#0d948822; color:#0d9488;">PSA</span>
              <div class="progress flex-grow-1 mx-2" style="height:8px;"><div class="progress-bar" style="width:73%; background:#0d9488;"></div></div>
              <span class="badge rounded-pill fs-6" style="background:#16a34a22; color:#16a34a;">RRA</span>
              <div class="progress flex-grow-1 mx-2" style="height:8px;"><div class="progress-bar" style="width:61%; background:#16a34a;"></div></div>
              <span class="badge rounded-pill fs-6" style="background:#e11d4822; color:#e11d48;">ACA</span>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="card">
          <div class="card-header bg-white border-0 pt-3"><h5 class="fw-bold"><i class="bi bi-pie-chart me-2" style="color:#4f46e5;"></i>Channel Distribution</h5></div>
          <div class="card-body">
            <div class="mb-2"><span style="color:#16a34a;">Voice IVR</span><div class="progress" style="height:20px;"><div class="progress-bar" style="width:45%; background:#16a34a;">45%</div></div></div>
            <div class="mb-2"><span style="color:#4f46e5;">SMS Deep-Link</span><div class="progress" style="height:20px;"><div class="progress-bar" style="width:35%; background:#4f46e5;">35%</div></div></div>
            <div><span style="color:#ea580c;">Callback</span><div class="progress" style="height:20px;"><div class="progress-bar" style="width:20%; background:#ea580c;">20%</div></div></div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  setupStatus: any = null;
  seeding = false;
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/system-setup';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.checkSetup(); }

  checkSetup() {
    this.http.get<any>(`${this.baseUrl}/status`).subscribe({
      next: (data) => { this.setupStatus = data; this.cdr.detectChanges(); },
      error: () => { this.setupStatus = null; this.cdr.detectChanges(); }
    });
  }

  seedAll() {
    this.seeding = true;
    this.http.post(`${this.baseUrl}/seed-all`, {}).subscribe({
      next: () => { this.seeding = false; this.checkSetup(); },
      error: () => { this.seeding = false; this.cdr.detectChanges(); }
    });
  }
}
