import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-workflow-runs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="mb-3">
      <a routerLink="/workflow-definitions" class="text-decoration-none" style="color: #ea580c;">
        <i class="bi bi-arrow-left me-1"></i>Back to Definitions
      </a>
    </div>

    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #ea580c;">
        <i class="bi bi-play-circle me-2"></i>Workflow Runs
      </h2>
      <button class="btn btn-sm" style="background: #ea580c22; color: #ea580c; border-radius: 8px;" (click)="loadRuns()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <!-- Submit Run Form -->
    <div class="card mb-4" style="border-left: 4px solid #ea580c;">
      <div class="card-header bg-white border-0 pt-3">
        <h5 class="fw-bold"><i class="bi bi-rocket-takeoff me-2" style="color: #ea580c;"></i>Submit New Run</h5>
      </div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-3">
            <label class="form-label fw-semibold">Workflow</label>
            <select class="form-select" [(ngModel)]="submitReq.workflowDefinitionId" (change)="onDefChange()">
              <option value="">Select workflow...</option>
              <option *ngFor="let d of definitions" [value]="d.id">{{ d.displayName }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label fw-semibold">Engine</label>
            <select class="form-select" [(ngModel)]="submitReq.engineId" [disabled]="compatibleEngines.length === 0">
              <option value="">Select engine...</option>
              <option *ngFor="let e of compatibleEngines" [value]="e.engineId">{{ e.engineName }} ({{ e.engineType }})</option>
            </select>
            <small *ngIf="submitReq.workflowDefinitionId && compatibleEngines.length === 0" class="text-danger">
              No engines mapped. Map engines in Definitions page first.
            </small>
          </div>
          <div class="col-md-3">
            <label class="form-label fw-semibold">Dataset Instance (optional)</label>
            <select class="form-select" [(ngModel)]="submitReq.datasetInstanceId">
              <option value="">None</option>
              <option *ngFor="let di of datasetInstances" [value]="di.id">
                #{{ di.id }} — {{ di.storageType }} ({{ di.status }})
              </option>
            </select>
          </div>
          <div class="col-md-3 d-flex align-items-end">
            <button class="btn w-100" style="background: #ea580c; color: white; border-radius: 8px;"
                    (click)="submitRun()" [disabled]="submitting || !submitReq.workflowDefinitionId || !submitReq.engineId">
              <i class="bi bi-rocket-takeoff me-1"></i>{{ submitting ? 'Submitting...' : 'Submit Run' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #ea580c;"></div>
    </div>

    <!-- Empty state -->
    <div *ngIf="!loading && runs.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-play-circle fs-1" style="color: #ea580c; opacity: 0.5;"></i>
        <h5 class="mt-3 fw-bold">No Runs Yet</h5>
        <p class="text-muted">Submit a workflow run above.</p>
      </div>
    </div>

    <!-- Runs table -->
    <div class="table-responsive" *ngIf="!loading && runs.length > 0">
      <table class="table table-hover align-middle">
        <thead>
          <tr style="color: #ea580c;">
            <th>ID</th>
            <th>Workflow</th>
            <th>Engine</th>
            <th>Dataset</th>
            <th>Status</th>
            <th>External ID</th>
            <th>Submitted</th>
            <th>Completed</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let r of runs">
            <td class="fw-semibold">#{{ r.id }}</td>
            <td>
              <span class="fw-semibold">{{ r.workflowDisplayName }}</span>
              <br><code class="small text-muted">{{ r.workflowKey }}</code>
            </td>
            <td><span class="badge" style="background: #f0fdf4; color: #059669;">{{ r.engineName }}</span>
              <small class="text-muted ms-1">{{ r.engineType }}</small>
            </td>
            <td>{{ r.datasetInstanceId ? '#' + r.datasetInstanceId : '—' }}</td>
            <td>
              <span class="badge"
                    [style.background]="statusColor(r.runStatus).bg"
                    [style.color]="statusColor(r.runStatus).fg">
                {{ r.runStatus }}
              </span>
            </td>
            <td class="small text-muted">{{ r.externalRunId || '—' }}</td>
            <td class="small">{{ r.submittedAt | date:'short' }}</td>
            <td class="small">{{ r.completedAt ? (r.completedAt | date:'short') : '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class WorkflowRunsComponent implements OnInit {
  runs: any[] = [];
  definitions: any[] = [];
  datasetInstances: any[] = [];
  compatibleEngines: any[] = [];
  loading = true;
  submitting = false;
  filterDefId: string = '';
  submitReq = { workflowDefinitionId: '', engineId: '', datasetInstanceId: '' };

  private runsUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-runs';
  private defsUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-definitions';
  private datasetsUrl = window.location.origin + '/smart-care/api/admin/v1/analytics/datasets';

  constructor(private http: HttpClient, private route: ActivatedRoute, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.filterDefId = this.route.snapshot.queryParamMap.get('definitionId') || '';
    if (this.filterDefId) this.submitReq.workflowDefinitionId = this.filterDefId;
    this.loadDefinitions();
    this.loadDatasetInstances();
    this.loadRuns();
  }

  loadDefinitions() {
    this.http.get<any[]>(this.defsUrl).subscribe({
      next: (data) => {
        this.definitions = data;
        if (this.filterDefId) this.onDefChange();
        this.cdr.detectChanges();
      }
    });
  }

  onDefChange() {
    const def = this.definitions.find(d => d.id === Number(this.submitReq.workflowDefinitionId));
    this.compatibleEngines = def?.engines || [];
    this.submitReq.engineId = '';
    this.cdr.detectChanges();
  }

  loadDatasetInstances() {
    this.http.get<any[]>(this.datasetsUrl).subscribe({
      next: (datasets) => {
        this.datasetInstances = [];
        for (const ds of datasets) {
          if (ds.instances) {
            for (const inst of ds.instances) {
              this.datasetInstances.push(inst);
            }
          }
        }
        this.cdr.detectChanges();
      }
    });
  }

  loadRuns() {
    this.loading = true;
    const url = this.filterDefId
      ? `${this.runsUrl}/by-definition/${this.filterDefId}`
      : this.runsUrl;
    this.http.get<any[]>(url).subscribe({
      next: (data) => { this.runs = data; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  submitRun() {
    this.submitting = true;
    const body: any = {
      workflowDefinitionId: Number(this.submitReq.workflowDefinitionId),
      engineId: Number(this.submitReq.engineId)
    };
    if (this.submitReq.datasetInstanceId) {
      body.datasetInstanceId = Number(this.submitReq.datasetInstanceId);
    }
    this.http.post(`${this.runsUrl}/submit`, body).subscribe({
      next: () => { this.submitting = false; this.loadRuns(); },
      error: () => { this.submitting = false; this.cdr.detectChanges(); }
    });
  }

  statusColor(status: string) {
    switch (status) {
      case 'SUBMITTED': return { bg: '#fef3c7', fg: '#d97706' };
      case 'RUNNING': return { bg: '#dbeafe', fg: '#2563eb' };
      case 'COMPLETED': return { bg: '#d1fae5', fg: '#059669' };
      case 'FAILED': return { bg: '#fee2e2', fg: '#dc2626' };
      default: return { bg: '#f3f4f6', fg: '#6b7280' };
    }
  }
}
