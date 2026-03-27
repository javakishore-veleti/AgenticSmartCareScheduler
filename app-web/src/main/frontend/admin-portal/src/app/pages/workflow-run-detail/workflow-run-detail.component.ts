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

    <!-- Loading -->
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
          <div class="d-flex align-items-center gap-2 mt-2">
            <span class="badge"
                  [style.background]="statusColor(run.runStatus).bg"
                  [style.color]="statusColor(run.runStatus).fg">
              {{ run.runStatus }}
            </span>
            <span class="badge" style="background: #f0fdf4; color: #059669;">
              {{ run.engineName }} ({{ run.engineType }})
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
            <div class="col-md-4">
              <small class="text-muted d-block">Submitted</small>
              <span class="fw-semibold">{{ run.submittedAt | date:'medium' }}</span>
            </div>
            <div class="col-md-4">
              <small class="text-muted d-block">Started</small>
              <span class="fw-semibold">{{ run.startedAt ? (run.startedAt | date:'medium') : '—' }}</span>
            </div>
            <div class="col-md-4">
              <small class="text-muted d-block">Completed</small>
              <span class="fw-semibold">{{ run.completedAt ? (run.completedAt | date:'medium') : '—' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Error box for FAILED -->
      <div *ngIf="run.runStatus === 'FAILED' && run.errorMessage" class="alert alert-danger mb-4">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <strong>Error:</strong> {{ run.errorMessage }}
      </div>

      <!-- Results section for COMPLETED -->
      <div *ngIf="run.runStatus === 'COMPLETED'">
        <div *ngIf="resultsLoading" class="text-center py-4">
          <div class="spinner-border spinner-border-sm" style="color: #ea580c;"></div>
          <span class="ms-2 text-muted">Loading results...</span>
        </div>

        <div *ngIf="!resultsLoading && !results" class="card mb-4">
          <div class="card-body text-center py-4">
            <i class="bi bi-hourglass-split fs-3 text-muted"></i>
            <p class="mt-2 text-muted">Results not yet available.</p>
          </div>
        </div>

        <div *ngIf="!resultsLoading && results">
          <h4 class="fw-bold mb-3" style="color: #ea580c;">
            <i class="bi bi-bar-chart-line me-2"></i>Results
          </h4>

          <!-- Summary Card -->
          <div class="card mb-4">
            <div class="card-header bg-white border-0 pt-3">
              <h5 class="fw-bold"><i class="bi bi-clipboard-data me-2" style="color: #ea580c;"></i>Summary</h5>
            </div>
            <div class="card-body">
              <div class="row text-center">
                <div class="col-md-6">
                  <div class="fs-2 fw-bold" style="color: #ea580c;">{{ results.summary?.totalRecords ?? '—' }}</div>
                  <small class="text-muted">Total Records</small>
                </div>
                <div class="col-md-6">
                  <div class="fs-2 fw-bold" style="color: #ea580c;">{{ formatPercent(results.summary?.noShowRate) }}</div>
                  <small class="text-muted">No-Show Rate</small>
                </div>
              </div>
            </div>
          </div>

          <!-- Context State Distribution -->
          <div *ngIf="results.contextStateDistribution" class="mb-4">
            <h5 class="fw-bold mb-3"><i class="bi bi-diagram-3 me-2" style="color: #ea580c;"></i>Context State Distribution</h5>
            <div class="row g-3">
              <div class="col-md-4" *ngFor="let cs of contextStates">
                <div class="card h-100 text-center">
                  <div class="card-body">
                    <h6 class="fw-bold text-muted">{{ cs.label }}</h6>
                    <div class="fs-3 fw-bold" style="color: #ea580c;">{{ cs.count }}</div>
                    <div class="text-muted">{{ formatPercent(cs.percentage) }} of total</div>
                    <div class="mt-2">
                      <small class="text-muted">No-Show Rate: </small>
                      <span class="fw-semibold">{{ formatPercent(cs.noShowRate) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Channel Distribution -->
          <div *ngIf="results.channelDistribution" class="mb-4">
            <h5 class="fw-bold mb-3"><i class="bi bi-broadcast me-2" style="color: #ea580c;"></i>Channel Distribution</h5>
            <div class="row g-3">
              <div class="col-md-4" *ngFor="let ch of channels">
                <div class="card h-100 text-center">
                  <div class="card-body">
                    <h6 class="fw-bold text-muted">{{ ch.label }}</h6>
                    <div class="fs-3 fw-bold" style="color: #ea580c;">{{ ch.count }}</div>
                    <div class="text-muted">{{ formatPercent(ch.percentage) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Baseline Comparison -->
          <div *ngIf="results.baselineComparison" class="card mb-4">
            <div class="card-header bg-white border-0 pt-3">
              <h5 class="fw-bold"><i class="bi bi-arrow-left-right me-2" style="color: #ea580c;"></i>Baseline Comparison</h5>
            </div>
            <div class="card-body">
              <div class="row text-center">
                <div class="col-md-4">
                  <small class="text-muted d-block">SMS-Only Reachability</small>
                  <span class="fs-4 fw-bold">{{ formatPercent(results.baselineComparison.smsOnlyReachability) }}</span>
                </div>
                <div class="col-md-4">
                  <small class="text-muted d-block">Proposed Reachability</small>
                  <span class="fs-4 fw-bold" style="color: #059669;">{{ formatPercent(results.baselineComparison.proposedReachability) }}</span>
                </div>
                <div class="col-md-4">
                  <small class="text-muted d-block">Improvement</small>
                  <span class="fs-4 fw-bold" style="color: #ea580c;">{{ formatPercent(results.baselineComparison.improvement) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Chart -->
          <div class="card mb-4">
            <div class="card-header bg-white border-0 pt-3">
              <h5 class="fw-bold"><i class="bi bi-pie-chart me-2" style="color: #ea580c;"></i>Channel Distribution Chart</h5>
            </div>
            <div class="card-body text-center">
              <img [src]="chartUrl" alt="Channel Distribution" class="img-fluid" style="max-width: 700px;"
                   (error)="chartError = true" *ngIf="!chartError" />
              <p *ngIf="chartError" class="text-muted">Chart image not available.</p>
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
        if (data.runStatus === 'COMPLETED') {
          this.loadResults();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadResults() {
    this.resultsLoading = true;
    this.http.get<any>(`${this.baseUrl}/${this.runId}/results`).subscribe({
      next: (data) => {
        this.results = data;
        this.buildContextStates();
        this.buildChannels();
        this.resultsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.results = null;
        this.resultsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  buildContextStates() {
    const dist = this.results?.contextStateDistribution;
    if (!dist) return;
    const labels: Record<string, string> = {
      REACHABLE_MOBILE: 'Reachable (Mobile)',
      REACHABLE_STATIONARY: 'Reachable (Stationary)',
      UNREACHABLE: 'Unreachable'
    };
    this.contextStates = Object.keys(dist).map(key => ({
      label: labels[key] || key,
      count: dist[key]?.count ?? 0,
      percentage: dist[key]?.percentage,
      noShowRate: dist[key]?.noShowRate
    }));
  }

  buildChannels() {
    const dist = this.results?.channelDistribution;
    if (!dist) return;
    const labels: Record<string, string> = {
      VOICE_IVR: 'Voice IVR',
      SMS_DEEPLINK: 'SMS Deeplink',
      CALLBACK: 'Callback'
    };
    this.channels = Object.keys(dist).map(key => ({
      label: labels[key] || key,
      count: dist[key]?.count ?? 0,
      percentage: dist[key]?.percentage
    }));
  }

  formatPercent(val: any): string {
    if (val == null) return '—';
    if (typeof val === 'number') {
      return val <= 1 ? (val * 100).toFixed(1) + '%' : val.toFixed(1) + '%';
    }
    return String(val);
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
