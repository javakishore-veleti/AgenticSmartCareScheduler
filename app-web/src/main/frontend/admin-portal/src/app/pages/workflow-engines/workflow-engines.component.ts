import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-workflow-engines',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #4f46e5;">
        <i class="bi bi-gear-wide-connected me-2"></i>Workflow Engines
      </h2>
      <button class="btn btn-sm" style="background: #4f46e522; color: #4f46e5; border-radius: 8px;" (click)="loadEngines()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <!-- Create Form -->
    <div class="card mb-4" style="border-left: 4px solid #4f46e5;">
      <div class="card-header bg-white border-0 pt-3">
        <h5 class="fw-bold"><i class="bi bi-plus-circle me-2" style="color: #4f46e5;"></i>Register Workflow Engine</h5>
      </div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-3">
            <label class="form-label fw-semibold">Engine Name</label>
            <input class="form-control" [(ngModel)]="newEngine.engineName" placeholder="e.g. local-airflow">
          </div>
          <div class="col-md-3">
            <label class="form-label fw-semibold">Engine Type</label>
            <select class="form-select" [(ngModel)]="newEngine.engineType">
              <option value="">Select type...</option>
              <option value="AIRFLOW">Apache Airflow</option>
              <option value="AWS_EMR">AWS EMR</option>
              <option value="AWS_STEP_FUNCTIONS">AWS Step Functions</option>
              <option value="DATABRICKS">Databricks</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label fw-semibold">Base URL</label>
            <input class="form-control" [(ngModel)]="newEngine.baseUrl" placeholder="http://localhost:8082">
          </div>
          <div class="col-md-3">
            <label class="form-label fw-semibold">Auth Type</label>
            <select class="form-select" [(ngModel)]="newEngine.authType">
              <option value="BASIC">Basic Auth</option>
              <option value="TOKEN">API Token</option>
              <option value="IAM">AWS IAM</option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label fw-semibold">Description</label>
            <input class="form-control" [(ngModel)]="newEngine.description" placeholder="Optional description">
          </div>
          <div class="col-12">
            <button class="btn" style="background: #4f46e5; color: white; border-radius: 8px;"
                    (click)="create()" [disabled]="creating || !newEngine.engineName || !newEngine.engineType">
              <i class="bi bi-plus-circle me-1"></i>{{ creating ? 'Creating...' : 'Register Engine' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #4f46e5;"></div>
    </div>

    <!-- Empty state -->
    <div *ngIf="!loading && engines.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-gear-wide-connected fs-1" style="color: #4f46e5; opacity: 0.5;"></i>
        <h5 class="mt-3 fw-bold">No Workflow Engines</h5>
        <p class="text-muted">Register an engine above to get started.</p>
      </div>
    </div>

    <!-- Engines table -->
    <div class="table-responsive" *ngIf="!loading && engines.length > 0">
      <table class="table table-hover align-middle">
        <thead>
          <tr style="color: #4f46e5;">
            <th>Name</th>
            <th>Type</th>
            <th>Base URL</th>
            <th>Auth</th>
            <th>Status</th>
            <th>Workflows</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let e of engines">
            <td class="fw-semibold">{{ e.engineName }}</td>
            <td><span class="badge" style="background: #4f46e522; color: #4f46e5;">{{ e.engineType }}</span></td>
            <td class="small text-muted">{{ e.baseUrl || '—' }}</td>
            <td class="small">{{ e.authType || '—' }}</td>
            <td>
              <span class="badge" [style.background]="e.status === 'ACTIVE' ? '#d1fae5' : '#fee2e2'"
                    [style.color]="e.status === 'ACTIVE' ? '#059669' : '#dc2626'">{{ e.status }}</span>
            </td>
            <td>
              <a [routerLink]="['/workflow-definitions']" [queryParams]="{engineId: e.id}"
                 class="btn btn-sm" style="background: #4f46e522; color: #4f46e5; border-radius: 6px;">
                <i class="bi bi-diagram-3 me-1"></i>View
              </a>
            </td>
            <td>
              <button class="btn btn-sm btn-outline-danger" (click)="deleteEngine(e.id)">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class WorkflowEnginesComponent implements OnInit {
  engines: any[] = [];
  loading = true;
  creating = false;
  newEngine = { engineName: '', engineType: '', baseUrl: '', authType: 'BASIC', description: '' };
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-engines';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadEngines(); }

  loadEngines() {
    this.loading = true;
    this.http.get<any[]>(this.baseUrl).subscribe({
      next: (data) => { this.engines = data; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  create() {
    this.creating = true;
    this.http.post(this.baseUrl, this.newEngine).subscribe({
      next: () => {
        this.creating = false;
        this.newEngine = { engineName: '', engineType: '', baseUrl: '', authType: 'BASIC', description: '' };
        this.loadEngines();
      },
      error: () => { this.creating = false; this.cdr.detectChanges(); }
    });
  }

  deleteEngine(id: number) {
    this.http.delete(`${this.baseUrl}/${id}`).subscribe({ next: () => this.loadEngines() });
  }
}
