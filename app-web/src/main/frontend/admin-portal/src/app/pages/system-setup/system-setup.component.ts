import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-system-setup',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mb-3">
      <a routerLink="/" class="text-decoration-none" style="color: #d97706;">
        <i class="bi bi-arrow-left me-1"></i>Back to Dashboard
      </a>
    </div>

    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #d97706;">
        <i class="bi bi-gear-wide me-2"></i>Foundational Data Setup
      </h2>
      <button class="btn btn-sm" style="background: #d9770622; color: #d97706; border-radius: 8px;" (click)="loadStatus()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <!-- Overall status -->
    <div *ngIf="status" class="alert mb-4" [style.background]="status.allSeeded ? '#d1fae5' : '#fef3c7'"
         [style.border-color]="status.allSeeded ? '#059669' : '#d97706'" style="border: 1px solid; border-radius: 12px;">
      <div class="d-flex align-items-center">
        <i class="bi fs-4 me-3" [class]="status.allSeeded ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'"
           [style.color]="status.allSeeded ? '#059669' : '#d97706'"></i>
        <div>
          <h5 class="fw-bold mb-0" [style.color]="status.allSeeded ? '#065f46' : '#92400e'">
            {{ status.allSeeded ? 'All Foundational Data Configured' : 'Setup Incomplete' }}
          </h5>
          <p class="mb-0 small" [style.color]="status.allSeeded ? '#047857' : '#78350f'">
            {{ status.allSeeded ? 'System is ready for use.' : status.pendingSetup.length + ' step(s) remaining.' }}
          </p>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div *ngIf="!status" class="text-center py-5">
      <div class="spinner-border" style="color: #d97706;"></div>
    </div>

    <!-- Setup steps -->
    <div *ngIf="status" class="row g-3">
      <!-- Datasets -->
      <div class="col-12">
        <div class="card" [style.border-left]="'4px solid ' + (status.datasetsSeeded ? '#059669' : '#d97706')">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
              <i class="bi fs-3 me-3" [class]="status.datasetsSeeded ? 'bi-check-circle-fill' : 'bi-circle'"
                 [style.color]="status.datasetsSeeded ? '#059669' : '#d97706'"></i>
              <div>
                <h5 class="fw-bold mb-1">Default Datasets</h5>
                <p class="text-muted small mb-0">Medical Appointment No-Show dataset definition (110,527 records, Kaggle)</p>
              </div>
            </div>
            <div>
              <span *ngIf="status.datasetsSeeded" class="badge" style="background: #d1fae5; color: #059669;">Seeded</span>
              <button *ngIf="!status.datasetsSeeded" class="btn btn-sm" style="background: #d97706; color: white; border-radius: 8px;"
                      (click)="seed('datasets')" [disabled]="seedingKey === 'datasets'">
                <i class="bi bi-cloud-download me-1"></i>{{ seedingKey === 'datasets' ? 'Seeding...' : 'Seed Now' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Workflow Definitions -->
      <div class="col-12">
        <div class="card" [style.border-left]="'4px solid ' + (status.workflowsSeeded ? '#059669' : '#d97706')">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
              <i class="bi fs-3 me-3" [class]="status.workflowsSeeded ? 'bi-check-circle-fill' : 'bi-circle'"
                 [style.color]="status.workflowsSeeded ? '#059669' : '#d97706'"></i>
              <div>
                <h5 class="fw-bold mb-1">Workflow Definitions</h5>
                <p class="text-muted small mb-0">5 agentic outreach workflows: Patient Outreach, Appointment Confirmation, Waitlist Fulfillment, Schedule Optimization, Compliance Audit</p>
              </div>
            </div>
            <div>
              <span *ngIf="status.workflowsSeeded" class="badge" style="background: #d1fae5; color: #059669;">Seeded</span>
              <button *ngIf="!status.workflowsSeeded" class="btn btn-sm" style="background: #d97706; color: white; border-radius: 8px;"
                      (click)="seed('workflows')" [disabled]="seedingKey === 'workflows'">
                <i class="bi bi-cloud-download me-1"></i>{{ seedingKey === 'workflows' ? 'Seeding...' : 'Seed Now' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Workflow Engines -->
      <div class="col-12">
        <div class="card" [style.border-left]="'4px solid ' + (status.enginesSeeded ? '#059669' : '#d97706')">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
              <i class="bi fs-3 me-3" [class]="status.enginesSeeded ? 'bi-check-circle-fill' : 'bi-circle'"
                 [style.color]="status.enginesSeeded ? '#059669' : '#d97706'"></i>
              <div>
                <h5 class="fw-bold mb-1">Workflow Engines</h5>
                <p class="text-muted small mb-0">Local Apache Airflow engine (Docker Compose on port 8082)</p>
              </div>
            </div>
            <div>
              <span *ngIf="status.enginesSeeded" class="badge" style="background: #d1fae5; color: #059669;">Seeded</span>
              <button *ngIf="!status.enginesSeeded" class="btn btn-sm" style="background: #d97706; color: white; border-radius: 8px;"
                      (click)="seed('engines')" [disabled]="seedingKey === 'engines'">
                <i class="bi bi-cloud-download me-1"></i>{{ seedingKey === 'engines' ? 'Seeding...' : 'Seed Now' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Workflow-Engine Mappings -->
      <div class="col-12">
        <div class="card" [style.border-left]="'4px solid ' + (status.mappingsSeeded ? '#059669' : '#d97706')">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center">
              <i class="bi fs-3 me-3" [class]="status.mappingsSeeded ? 'bi-check-circle-fill' : 'bi-circle'"
                 [style.color]="status.mappingsSeeded ? '#059669' : '#d97706'"></i>
              <div>
                <h5 class="fw-bold mb-1">Workflow-Engine Mappings</h5>
                <p class="text-muted small mb-0">Map all workflows to all compatible engines so they appear in the engine dropdown when creating a run</p>
              </div>
            </div>
            <div>
              <span *ngIf="status.mappingsSeeded" class="badge" style="background: #d1fae5; color: #059669;">Seeded</span>
              <button *ngIf="!status.mappingsSeeded" class="btn btn-sm" style="background: #d97706; color: white; border-radius: 8px;"
                      (click)="seed('mappings')" [disabled]="seedingKey === 'mappings'">
                <i class="bi bi-cloud-download me-1"></i>{{ seedingKey === 'mappings' ? 'Seeding...' : 'Seed Now' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Setup log -->
    <div *ngIf="logs.length > 0" class="mt-4">
      <h5 class="fw-bold"><i class="bi bi-journal-text me-2" style="color: #d97706;"></i>Setup Activity Log</h5>
      <div class="table-responsive">
        <table class="table table-hover align-middle small">
          <thead><tr style="color: #d97706;"><th>Setting</th><th>Activity</th><th>Details</th><th>By</th><th>When</th></tr></thead>
          <tbody>
            <tr *ngFor="let l of logs">
              <td class="fw-semibold">{{ l.settingKey }}</td>
              <td><span class="badge" style="background: #d1fae5; color: #059669;">{{ l.activityType }}</span></td>
              <td class="text-muted">{{ l.details }}</td>
              <td>{{ l.performedBy }}</td>
              <td>{{ l.createdAt | date:'short' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class SystemSetupComponent implements OnInit {
  status: any = null;
  logs: any[] = [];
  seedingKey = '';
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/system-setup';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadStatus(); this.loadLogs(); }

  loadStatus() {
    this.http.get<any>(`${this.baseUrl}/status`).subscribe({
      next: (data) => { this.status = data; this.cdr.detectChanges(); }
    });
  }

  loadLogs() {
    this.http.get<any[]>(`${this.baseUrl}/logs`).subscribe({
      next: (data) => { this.logs = data; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  seed(key: string) {
    this.seedingKey = key;
    this.http.post(`${this.baseUrl}/seed/${key}`, {}).subscribe({
      next: () => { this.seedingKey = ''; this.loadStatus(); this.loadLogs(); },
      error: () => { this.seedingKey = ''; this.cdr.detectChanges(); }
    });
  }
}
