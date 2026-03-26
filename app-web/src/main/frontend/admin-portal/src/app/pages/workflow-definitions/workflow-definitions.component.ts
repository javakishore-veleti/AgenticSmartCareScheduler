import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-workflow-definitions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #7c3aed;">
        <i class="bi bi-diagram-3 me-2"></i>Workflow Definitions
      </h2>
      <div class="d-flex gap-2">
        <button class="btn btn-sm" style="background: #7c3aed; color: white; border-radius: 8px;"
                (click)="seedDefaults()" [disabled]="seeding">
          <i class="bi bi-cloud-download me-1"></i>{{ seeding ? 'Seeding...' : 'Register Default Workflows' }}
        </button>
        <button class="btn btn-sm" style="background: #7c3aed22; color: #7c3aed; border-radius: 8px;" (click)="loadDefinitions()">
          <i class="bi bi-arrow-clockwise me-1"></i>Refresh
        </button>
      </div>
    </div>

    <!-- Success message -->
    <div *ngIf="successMsg" class="alert alert-success alert-dismissible fade show">
      {{ successMsg }}
      <button type="button" class="btn-close" (click)="successMsg = ''"></button>
    </div>

    <!-- Create Form -->
    <div class="card mb-4" style="border-left: 4px solid #7c3aed;">
      <div class="card-header bg-white border-0 pt-3">
        <h5 class="fw-bold"><i class="bi bi-plus-circle me-2" style="color: #7c3aed;"></i>Register Custom Workflow</h5>
      </div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label fw-semibold">Workflow Key</label>
            <input class="form-control" [(ngModel)]="newDef.workflowKey" placeholder="e.g. custom_etl_pipeline">
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Display Name</label>
            <input class="form-control" [(ngModel)]="newDef.displayName" placeholder="e.g. Custom ETL Pipeline">
          </div>
          <div class="col-md-4 d-flex align-items-end">
            <button class="btn w-100" style="background: #7c3aed; color: white; border-radius: 8px;"
                    (click)="create()" [disabled]="creating || !newDef.workflowKey || !newDef.displayName">
              <i class="bi bi-plus-circle me-1"></i>{{ creating ? 'Creating...' : 'Register' }}
            </button>
          </div>
          <div class="col-12">
            <label class="form-label fw-semibold">Description</label>
            <input class="form-control" [(ngModel)]="newDef.description" placeholder="What does this workflow do?">
          </div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #7c3aed;"></div>
    </div>

    <!-- Empty state -->
    <div *ngIf="!loading && definitions.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-diagram-3 fs-1" style="color: #7c3aed; opacity: 0.5;"></i>
        <h5 class="mt-3 fw-bold">No Workflow Definitions</h5>
        <p class="text-muted">Click "Register Default Workflows" to seed the product workflows, or register a custom one above.</p>
      </div>
    </div>

    <!-- Definitions cards -->
    <div class="row g-3" *ngIf="!loading && definitions.length > 0">
      <div class="col-12" *ngFor="let d of definitions">
        <div class="card" style="border-left: 4px solid #7c3aed;">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <div>
                <h5 class="fw-bold mb-1" style="color: #7c3aed;">{{ d.displayName }}</h5>
                <code class="small" style="color: #4f46e5;">{{ d.workflowKey }}</code>
              </div>
              <div>
                <span class="badge" [style.background]="d.status === 'ACTIVE' ? '#d1fae5' : '#fee2e2'"
                      [style.color]="d.status === 'ACTIVE' ? '#059669' : '#dc2626'">{{ d.status }}</span>
                <button class="btn btn-sm btn-outline-danger ms-2" (click)="deleteDef(d.id)">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
            </div>
            <p *ngIf="d.description" class="text-muted small mb-2">{{ d.description }}</p>

            <!-- Metadata badges -->
            <div class="d-flex flex-wrap gap-2 mb-3" *ngIf="d.agentPipeline || d.awsServices || d.techStack || d.paperSection">
              <span *ngIf="d.agentPipeline" class="badge" style="background: #ede9fe; color: #7c3aed;">
                <i class="bi bi-robot me-1"></i>{{ d.agentPipeline }}
              </span>
              <span *ngIf="d.awsServices" class="badge" style="background: #fef3c7; color: #d97706;">
                <i class="bi bi-cloud me-1"></i>{{ d.awsServices }}
              </span>
              <span *ngIf="d.techStack" class="badge" style="background: #dbeafe; color: #2563eb;">
                <i class="bi bi-code-slash me-1"></i>{{ d.techStack }}
              </span>
              <span *ngIf="d.paperSection" class="badge" style="background: #d1fae5; color: #059669;">
                <i class="bi bi-journal-text me-1"></i>{{ d.paperSection }}
              </span>
            </div>

            <!-- Compatible Engines -->
            <div class="mb-2">
              <span class="fw-semibold small">Compatible Engines:</span>
              <span *ngIf="!d.engines || d.engines.length === 0" class="text-muted small ms-2">None mapped yet</span>
              <span *ngFor="let eng of d.engines" class="badge me-1 ms-1" style="background: #f0fdf4; color: #059669;">
                {{ eng.engineName }} ({{ eng.engineType }})
                <span *ngIf="eng.engineWorkflowRef" class="text-muted"> — {{ eng.engineWorkflowRef }}</span>
                <i class="bi bi-x-circle ms-1" style="cursor:pointer;" (click)="removeMapping(eng.mappingId)"></i>
              </span>
            </div>

            <!-- Add Engine Mapping -->
            <div class="d-flex gap-2 align-items-end mt-2" *ngIf="engines.length > 0">
              <select class="form-select form-select-sm" style="max-width:200px;" [(ngModel)]="addMappingState[d.id].engineId">
                <option value="">Add engine...</option>
                <option *ngFor="let e of engines" [value]="e.id">{{ e.engineName }}</option>
              </select>
              <input class="form-control form-control-sm" style="max-width:200px;"
                     [(ngModel)]="addMappingState[d.id].engineWorkflowRef" placeholder="DAG id / step name">
              <button class="btn btn-sm" style="background: #059669; color: white; border-radius: 6px;"
                      (click)="addMapping(d.id)" [disabled]="!addMappingState[d.id].engineId">
                <i class="bi bi-link-45deg"></i> Map
              </button>
              <a [routerLink]="['/workflow-runs']" [queryParams]="{definitionId: d.id}"
                 class="btn btn-sm ms-auto" style="background: #ea580c; color: white; border-radius: 6px;">
                <i class="bi bi-play-circle me-1"></i>View Runs
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class WorkflowDefinitionsComponent implements OnInit {
  definitions: any[] = [];
  engines: any[] = [];
  loading = true;
  creating = false;
  seeding = false;
  successMsg = '';
  newDef = { workflowKey: '', displayName: '', description: '' };
  addMappingState: { [defId: number]: { engineId: string; engineWorkflowRef: string } } = {};

  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-definitions';
  private engineUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-engines';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadEngines();
    this.loadDefinitions();
  }

  loadEngines() {
    this.http.get<any[]>(this.engineUrl).subscribe({
      next: (data) => { this.engines = data; this.cdr.detectChanges(); }
    });
  }

  loadDefinitions() {
    this.loading = true;
    this.http.get<any[]>(this.baseUrl).subscribe({
      next: (data) => {
        this.definitions = data;
        for (const d of data) {
          if (!this.addMappingState[d.id]) {
            this.addMappingState[d.id] = { engineId: '', engineWorkflowRef: '' };
          }
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  seedDefaults() {
    this.seeding = true;
    this.http.post(`${this.baseUrl}/seed-defaults`, {}).subscribe({
      next: () => {
        this.seeding = false;
        this.successMsg = 'Default workflows registered successfully!';
        this.loadDefinitions();
      },
      error: () => { this.seeding = false; this.cdr.detectChanges(); }
    });
  }

  create() {
    this.creating = true;
    this.http.post(this.baseUrl, this.newDef).subscribe({
      next: () => {
        this.creating = false;
        this.newDef = { workflowKey: '', displayName: '', description: '' };
        this.loadDefinitions();
      },
      error: () => { this.creating = false; this.cdr.detectChanges(); }
    });
  }

  deleteDef(id: number) {
    this.http.delete(`${this.baseUrl}/${id}`).subscribe({ next: () => this.loadDefinitions() });
  }

  addMapping(defId: number) {
    const state = this.addMappingState[defId];
    this.http.post(`${this.baseUrl}/${defId}/engines`, {
      engineId: Number(state.engineId),
      engineWorkflowRef: state.engineWorkflowRef || null
    }).subscribe({
      next: () => {
        this.addMappingState[defId] = { engineId: '', engineWorkflowRef: '' };
        this.loadDefinitions();
      }
    });
  }

  removeMapping(mappingId: number) {
    this.http.delete(`${this.baseUrl}/engine-mappings/${mappingId}`).subscribe({
      next: () => this.loadDefinitions()
    });
  }
}
