import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-workflow-definitions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #7c3aed;">
        <i class="bi bi-diagram-3 me-2"></i>Workflow Catalog
      </h2>
      <button class="btn btn-sm" style="background: #7c3aed22; color: #7c3aed; border-radius: 8px;" (click)="loadDefinitions()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <p class="text-muted mb-4">Product workflows for agentic patient outreach. Select a workflow and go to <a routerLink="/workflow-runs" style="color: #ea580c;">Runs</a> to execute it.</p>

    <!-- Loading -->
    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #7c3aed;"></div>
    </div>

    <!-- Empty state -->
    <div *ngIf="!loading && definitions.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-diagram-3 fs-1" style="color: #7c3aed; opacity: 0.5;"></i>
        <h5 class="mt-3 fw-bold">No Workflow Definitions</h5>
        <p class="text-muted">Go to <a routerLink="/system-setup" style="color: #d97706;">System Setup</a> to seed foundational data.</p>
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
                <span *ngIf="d.requiresDataset" class="badge ms-2" style="background: #fee2e2; color: #dc2626;">
                  <i class="bi bi-database me-1"></i>Requires Dataset
                </span>
              </div>
              <a [routerLink]="['/workflow-runs']" [queryParams]="{definitionId: d.id}"
                 class="btn btn-sm" style="background: #ea580c; color: white; border-radius: 8px;">
                <i class="bi bi-play-circle me-1"></i>Run This Workflow
              </a>
            </div>
            <p *ngIf="d.description" class="text-muted small mb-2">{{ d.description }}</p>

            <!-- Metadata badges -->
            <div class="d-flex flex-wrap gap-2 mb-2">
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

            <!-- Compatible Engines (read-only) -->
            <div *ngIf="d.engines && d.engines.length > 0">
              <span class="fw-semibold small text-muted">Runs on:</span>
              <span *ngFor="let eng of d.engines" class="badge me-1 ms-1" style="background: #f0fdf4; color: #059669;">
                {{ eng.engineName }} ({{ eng.engineType }})
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class WorkflowDefinitionsComponent implements OnInit {
  definitions: any[] = [];
  loading = true;
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-definitions';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadDefinitions(); }

  loadDefinitions() {
    this.loading = true;
    this.http.get<any[]>(this.baseUrl).subscribe({
      next: (data) => { this.definitions = data; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }
}
