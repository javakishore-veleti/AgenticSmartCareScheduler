import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-workflow-run-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mb-3">
      <a routerLink="/workflow-runs" class="text-decoration-none" style="color: #ea580c;">
        <i class="bi bi-arrow-left me-1"></i>Back to Workflow Runs
      </a>
    </div>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #ea580c;"></div>
    </div>

    <div *ngIf="!loading && run">
      <!-- Header -->
      <div class="d-flex justify-content-between align-items-start mb-4">
        <div>
          <h2 class="fw-bold" style="color: #ea580c;">
            <i class="bi bi-play-circle me-2"></i>Run #{{ run.id }} &mdash; {{ run.workflowDisplayName }}
          </h2>
          <code class="small text-muted">{{ run.workflowKey }}</code>
          <div class="d-flex align-items-center gap-2 mt-2">
            <span class="badge fs-6"
                  [style.background]="statusColor(run.runStatus).bg"
                  [style.color]="statusColor(run.runStatus).fg">
              {{ run.runStatus }}
            </span>
            <a [href]="'http://localhost:8082'" target="_blank" class="badge text-decoration-none fs-6"
               style="background: #f0fdf4; color: #059669; cursor: pointer;">
              <i class="bi bi-box-arrow-up-right me-1"></i>{{ run.engineName }} ({{ run.engineType }})
            </a>
            <span *ngIf="run.externalRunId" class="badge" style="background: #e0e7ff; color: #4f46e5;">
              External: {{ run.externalRunId }}
            </span>
          </div>
        </div>
        <button class="btn btn-sm" style="background: #ea580c22; color: #ea580c; border-radius: 8px;" (click)="loadRun()">
          <i class="bi bi-arrow-clockwise me-1"></i>Refresh
        </button>
      </div>

      <!-- Timestamps -->
      <div class="card mb-4" style="border-left: 4px solid #ea580c;">
        <div class="card-body">
          <div class="row">
            <div class="col-md-3">
              <small class="text-muted d-block">Submitted</small>
              <span class="fw-semibold">{{ run.submittedAt | date:'medium' }}</span>
            </div>
            <div class="col-md-3">
              <small class="text-muted d-block">Started</small>
              <span class="fw-semibold">{{ run.startedAt ? (run.startedAt | date:'medium') : '—' }}</span>
            </div>
            <div class="col-md-3">
              <small class="text-muted d-block">Completed</small>
              <span class="fw-semibold">{{ run.completedAt ? (run.completedAt | date:'medium') : '—' }}</span>
            </div>
            <div class="col-md-3">
              <small class="text-muted d-block">Dataset Instance</small>
              <span class="fw-semibold">{{ run.datasetInstanceId ? '#' + run.datasetInstanceId : 'None' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Error for FAILED -->
      <div *ngIf="run.runStatus === 'FAILED'" class="alert mb-4"
           style="background: #fee2e2; border: 1px solid #dc2626; border-radius: 12px;">
        <h5 class="fw-bold" style="color: #991b1b;">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>Workflow Run Failed
        </h5>
        <pre *ngIf="run.errorMessage" class="mb-0 small" style="color: #991b1b; white-space: pre-wrap;">{{ run.errorMessage }}</pre>
        <p *ngIf="!run.errorMessage" class="mb-0 text-muted">No error details available. Check Airflow logs.</p>
      </div>

      <!-- SUBMITTED / RUNNING -->
      <div *ngIf="run.runStatus === 'SUBMITTED' || run.runStatus === 'RUNNING'" class="card mb-4">
        <div class="card-body text-center py-5">
          <div class="spinner-border mb-3" style="color: #ea580c;"></div>
          <h5 class="fw-bold">Workflow is {{ run.runStatus === 'SUBMITTED' ? 'queued' : 'running' }}...</h5>
          <p class="text-muted">Results will appear here when complete. Click Refresh to check.</p>
        </div>
      </div>

      <!-- Results for COMPLETED -->
      <div *ngIf="run.runStatus === 'COMPLETED'">
        <div *ngIf="resultsLoading" class="text-center py-4">
          <div class="spinner-border spinner-border-sm" style="color: #ea580c;"></div>
        </div>

        <div *ngIf="!resultsLoading && !results" class="card mb-4">
          <div class="card-body text-center py-4">
            <i class="bi bi-hourglass-split fs-3 text-muted"></i>
            <p class="mt-2 text-muted">Results file not found.</p>
          </div>
        </div>

        <div *ngIf="!resultsLoading && results">
          <div class="accordion mb-3" id="accordionResults">

            <!-- Accordion: Problem & Approach -->
            <div class="accordion-item" *ngIf="results.narrative">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseNarr">
                  <i class="bi bi-lightbulb me-2" style="color: #d97706;"></i>
                  <strong>Problem, Approach & Value</strong>
                </button>
              </h2>
              <div id="collapseNarr" class="accordion-collapse collapse show">
                <div class="accordion-body">
                  <div class="mb-3" *ngIf="results.narrative.problem_statement">
                    <h6 class="fw-bold" style="color: #dc2626;"><i class="bi bi-exclamation-circle me-1"></i>Problem</h6>
                    <p class="mb-0">{{ results.narrative.problem_statement }}</p>
                  </div>
                  <div class="mb-3" *ngIf="results.narrative.what_agents_did">
                    <h6 class="fw-bold" style="color: #4f46e5;"><i class="bi bi-robot me-1"></i>What the Agents Did</h6>
                    <p class="mb-0">{{ results.narrative.what_agents_did }}</p>
                  </div>
                  <div class="mb-3" *ngIf="results.narrative.key_finding">
                    <h6 class="fw-bold" style="color: #ea580c;"><i class="bi bi-search me-1"></i>Key Finding</h6>
                    <p class="mb-0">{{ results.narrative.key_finding }}</p>
                  </div>
                  <div class="mb-3" *ngIf="results.narrative.value_proposition">
                    <h6 class="fw-bold" style="color: #059669;"><i class="bi bi-graph-up-arrow me-1"></i>Value Proposition</h6>
                    <p class="mb-0">{{ results.narrative.value_proposition }}</p>
                  </div>
                  <div *ngIf="results.narrative.clinical_impact">
                    <h6 class="fw-bold" style="color: #7c3aed;"><i class="bi bi-heart-pulse me-1"></i>Clinical Impact</h6>
                    <p class="mb-0">{{ results.narrative.clinical_impact }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Input Dataset -->
            <div class="accordion-item" *ngIf="results.input_dataset">
              <h2 class="accordion-header">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseDs">
                  <i class="bi bi-database me-2" style="color: #7c3aed;"></i>
                  <strong>Input Dataset</strong>
                  <span class="badge ms-2" style="background: #7c3aed22; color: #7c3aed;">{{ results.input_dataset.source }}</span>
                </button>
              </h2>
              <div id="collapseDs" class="accordion-collapse collapse">
                <div class="accordion-body">
                  <div class="row g-3">
                    <div class="col-md-3 text-center">
                      <div class="fs-3 fw-bold" style="color: #4f46e5;">{{ results.input_dataset.records | number }}</div>
                      <small class="text-muted">Records</small>
                    </div>
                    <div class="col-md-3 text-center">
                      <div class="fs-3 fw-bold" style="color: #dc2626;">{{ results.input_dataset.no_show_count | number }}</div>
                      <small class="text-muted">No-Shows</small>
                    </div>
                    <div class="col-md-3 text-center">
                      <div class="fs-3 fw-bold" style="color: #059669;">{{ results.input_dataset.show_count | number }}</div>
                      <small class="text-muted">Shows</small>
                    </div>
                    <div class="col-md-3 text-center">
                      <div class="fs-3 fw-bold" style="color: #ea580c;">{{ results.no_show_rate }}%</div>
                      <small class="text-muted">No-Show Rate</small>
                    </div>
                  </div>
                  <div class="mt-3 small text-muted">
                    <strong>Features:</strong> {{ results.input_dataset.features_used }}<br>
                    <strong>Target:</strong> {{ results.input_dataset.target }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Summary -->
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseSum">
                  <i class="bi bi-clipboard-data me-2" style="color: #ea580c;"></i>
                  <strong>Summary</strong>
                  <span class="badge ms-2" style="background: #ea580c22; color: #ea580c;">{{ results.total_records | number }} records</span>
                </button>
              </h2>
              <div id="collapseSum" class="accordion-collapse collapse show">
                <div class="accordion-body">
                  <div class="row text-center">
                    <div class="col-md-4">
                      <div class="fs-2 fw-bold" style="color: #4f46e5;">{{ results.total_records | number }}</div>
                      <small class="text-muted">Total Records</small>
                    </div>
                    <div class="col-md-4">
                      <div class="fs-2 fw-bold" style="color: #ea580c;">{{ results.no_show_rate }}%</div>
                      <small class="text-muted">No-Show Rate</small>
                    </div>
                    <div class="col-md-4">
                      <div class="fs-2 fw-bold" style="color: #059669;">+{{ results.baseline_comparison?.improvement_pp }}pp</div>
                      <small class="text-muted">Reachability Improvement</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Context States -->
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseCp">
                  <i class="bi bi-diagram-3 me-2" style="color: #7c3aed;"></i>
                  <strong>Patient Context States (C_p)</strong>
                </button>
              </h2>
              <div id="collapseCp" class="accordion-collapse collapse show">
                <div class="accordion-body">
                  <div class="row g-3">
                    <div class="col-md-4" *ngFor="let cs of contextStates">
                      <div class="card text-center" [style.border-top]="'3px solid ' + cs.color">
                        <div class="card-body">
                          <h6 class="fw-bold" [style.color]="cs.color">{{ cs.label }}</h6>
                          <div class="fs-3 fw-bold">{{ cs.count | number }}</div>
                          <div class="text-muted">{{ cs.percentage }}% of total</div>
                          <div class="mt-2 small">
                            <span class="text-muted">No-show rate: </span>
                            <span class="fw-bold" [style.color]="cs.noShowRate > 40 ? '#dc2626' : '#059669'">{{ cs.noShowRate }}%</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Channels -->
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseCh">
                  <i class="bi bi-broadcast me-2" style="color: #16a34a;"></i>
                  <strong>COA Channel Selection</strong>
                </button>
              </h2>
              <div id="collapseCh" class="accordion-collapse collapse show">
                <div class="accordion-body">
                  <div class="row g-3">
                    <div class="col-md-4" *ngFor="let ch of channels">
                      <div class="card text-center" [style.border-top]="'3px solid ' + ch.color">
                        <div class="card-body">
                          <h6 class="fw-bold" [style.color]="ch.color">{{ ch.label }}</h6>
                          <div class="fs-3 fw-bold">{{ ch.count | number }}</div>
                          <div class="text-muted">{{ ch.percentage }}%</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Baseline Comparison -->
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseBl">
                  <i class="bi bi-arrow-left-right me-2" style="color: #ea580c;"></i>
                  <strong>Baseline Comparison — SMS-Only vs Proposed</strong>
                </button>
              </h2>
              <div id="collapseBl" class="accordion-collapse collapse show">
                <div class="accordion-body">
                  <div class="row text-center">
                    <div class="col-md-4">
                      <small class="text-muted d-block">SMS-Only Baseline</small>
                      <span class="fs-3 fw-bold" style="color: #94a3b8;">{{ results.baseline_comparison?.sms_only_reachability }}%</span>
                    </div>
                    <div class="col-md-4">
                      <small class="text-muted d-block">Proposed Multi-Channel</small>
                      <span class="fs-3 fw-bold" style="color: #4f46e5;">{{ results.baseline_comparison?.proposed_reachability }}%</span>
                    </div>
                    <div class="col-md-4">
                      <small class="text-muted d-block">Improvement</small>
                      <span class="fs-3 fw-bold" style="color: #059669;">+{{ results.baseline_comparison?.improvement_pp }}pp</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Accordion: Chart -->
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseChart">
                  <i class="bi bi-pie-chart me-2" style="color: #4f46e5;"></i>
                  <strong>Visualization</strong>
                </button>
              </h2>
              <div id="collapseChart" class="accordion-collapse collapse show">
                <div class="accordion-body text-center">
                  <img [src]="chartUrl" alt="Channel Distribution" class="img-fluid" style="max-width: 900px;"
                       (error)="chartError = true" *ngIf="!chartError" />
                  <p *ngIf="chartError" class="text-muted">Chart not available.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class WorkflowRunDetailComponent implements OnInit {
  run: any = null;
  results: any = null;
  loading = true;
  resultsLoading = false;
  chartError = false;
  contextStates: any[] = [];
  channels: any[] = [];
  chartUrl = '';

  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/workflow-runs';
  private runId!: string;

  constructor(private http: HttpClient, private route: ActivatedRoute, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.runId = this.route.snapshot.paramMap.get('id') || '';
    this.loadRun();
  }

  loadRun() {
    this.loading = true;
    this.http.get<any>(`${this.baseUrl}/${this.runId}`).subscribe({
      next: (data) => {
        this.run = data;
        this.loading = false;
        this.chartUrl = `${this.baseUrl}/${this.runId}/chart/channel_distribution.png`;
        if (data.runStatus === 'COMPLETED') this.loadResults();
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  loadResults() {
    this.resultsLoading = true;
    this.http.get<any>(`${this.baseUrl}/${this.runId}/results`).subscribe({
      next: (data) => {
        this.results = data;
        this.buildContextStates(data);
        this.buildChannels(data);
        this.resultsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.resultsLoading = false; this.cdr.detectChanges(); }
    });
  }

  buildContextStates(data: any) {
    const dist = data.context_state_distribution || {};
    const rates = data.noshow_rate_by_context || {};
    const meta: Record<string, { label: string; color: string }> = {
      REACHABLE_MOBILE: { label: 'Reachable (Mobile)', color: '#4f46e5' },
      REACHABLE_STATIONARY: { label: 'Reachable (Stationary)', color: '#16a34a' },
      UNREACHABLE: { label: 'Unreachable', color: '#ea580c' }
    };
    this.contextStates = Object.keys(dist).map(k => ({
      label: meta[k]?.label || k,
      color: meta[k]?.color || '#6b7280',
      count: dist[k]?.count || 0,
      percentage: dist[k]?.percentage || 0,
      noShowRate: rates[k] || 0
    }));
  }

  buildChannels(data: any) {
    const dist = data.channel_distribution || {};
    const meta: Record<string, { label: string; color: string }> = {
      VOICE_IVR: { label: 'Voice IVR', color: '#4f46e5' },
      SMS_DEEPLINK: { label: 'SMS Deep-Link', color: '#16a34a' },
      CALLBACK: { label: 'Callback', color: '#ea580c' }
    };
    this.channels = Object.keys(dist).map(k => ({
      label: meta[k]?.label || k,
      color: meta[k]?.color || '#6b7280',
      count: dist[k]?.count || 0,
      percentage: dist[k]?.percentage || 0
    }));
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
