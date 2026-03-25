import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DatasetService, DatasetDetails } from '../../services/dataset.service';

@Component({
  selector: 'app-datasets',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #4f46e5;">
        <i class="bi bi-database me-2"></i>Datasets
      </h2>
      <button class="btn" style="background: linear-gradient(135deg, #0d9488, #4f46e5); color: white; border-radius: 12px;"
              (click)="seedDefaults()" [disabled]="seeding">
        <i class="bi bi-plus-circle me-1"></i>
        {{ seeding ? 'Registering...' : 'Register Default Datasets' }}
      </button>
    </div>

    <!-- Seed success message -->
    <div *ngIf="seedMessage" class="alert alert-success alert-dismissible fade show" role="alert">
      <i class="bi bi-check-circle me-2"></i>{{ seedMessage }}
      <button type="button" class="btn-close" (click)="seedMessage = ''"></button>
    </div>

    <!-- Loading -->
    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #4f46e5;" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="mt-2 text-muted">Loading datasets...</p>
    </div>

    <!-- No datasets -->
    <div *ngIf="!loading && datasets.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-database-x fs-1" style="color: #ea580c;"></i>
        <h5 class="mt-3 fw-bold">No Datasets Registered</h5>
        <p class="text-muted">Click "Register Default Datasets" to add the Medical Appointment No-Show dataset.</p>
      </div>
    </div>

    <!-- Dataset cards -->
    <div *ngFor="let ds of datasets" class="card mb-4">
      <div class="card-header bg-white border-0 pt-3 d-flex justify-content-between align-items-start">
        <div>
          <h5 class="fw-bold mb-1" style="color: #0d9488;">
            <i class="bi bi-file-earmark-medical me-2"></i>{{ ds.displayName }}
          </h5>
          <span class="badge rounded-pill me-1" style="background: #4f46e522; color: #4f46e5;">{{ ds.datasetCode }}</span>
          <span class="badge rounded-pill me-1" style="background: #0d948822; color: #0d9488;">{{ ds.sourceProvider }}</span>
          <span class="badge rounded-pill" style="background: #16a34a22; color: #16a34a;">{{ ds.licenseType }}</span>
        </div>
        <div class="text-end">
          <span class="badge fs-6 rounded-pill"
                [style.background]="ds.instances.length > 0 ? '#16a34a22' : '#ea580c22'"
                [style.color]="ds.instances.length > 0 ? '#16a34a' : '#ea580c'">
            <i class="bi" [class.bi-check-circle]="ds.instances.length > 0" [class.bi-x-circle]="ds.instances.length === 0"></i>
            {{ ds.instances.length > 0 ? ds.instances.length + ' instance(s)' : 'Not downloaded' }}
          </span>
        </div>
      </div>
      <div class="card-body">
        <p class="text-muted small">{{ ds.description }}</p>

        <div class="row g-3 mb-3">
          <div class="col-md-2">
            <div class="p-2 rounded-3 text-center" style="background: #f0fdfa;">
              <p class="small text-muted mb-0">Records</p>
              <h5 class="fw-bold mb-0" style="color: #0d9488;">{{ ds.recordCount | number }}</h5>
            </div>
          </div>
          <div class="col-md-2">
            <div class="p-2 rounded-3 text-center" style="background: #e0e7ff;">
              <p class="small text-muted mb-0">Columns</p>
              <h5 class="fw-bold mb-0" style="color: #4f46e5;">{{ ds.columnCount }}</h5>
            </div>
          </div>
          <div class="col-md-2">
            <div class="p-2 rounded-3 text-center" style="background: #fef3c7;">
              <p class="small text-muted mb-0">Format</p>
              <h5 class="fw-bold mb-0 small" style="color: #d97706;">{{ ds.defaultFormat }}</h5>
            </div>
          </div>
          <div class="col-md-3">
            <div class="p-2 rounded-3 text-center" style="background: #fce7f3;">
              <p class="small text-muted mb-0">Source</p>
              <a [href]="ds.sourceUrl" target="_blank" class="small fw-bold" style="color: #e11d48; text-decoration: none;">
                <i class="bi bi-box-arrow-up-right me-1"></i>{{ ds.sourceProvider }}
              </a>
            </div>
          </div>
          <div class="col-md-3">
            <div class="p-2 rounded-3 text-center" style="background: #f0fdf4;">
              <p class="small text-muted mb-0">Tags</p>
              <span *ngFor="let tag of ds.tags?.split(',')" class="badge me-1" style="background: #16a34a22; color: #16a34a; font-size: 0.7em;">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>

        <!-- Instances table -->
        <div *ngIf="ds.instances.length > 0">
          <h6 class="fw-bold" style="color: #4f46e5;"><i class="bi bi-hdd-stack me-2"></i>Storage Instances</h6>
          <div class="table-responsive">
            <table class="table table-hover table-sm align-middle">
              <thead>
                <tr style="color: #4f46e5;">
                  <th>Storage</th>
                  <th>Format</th>
                  <th>Status</th>
                  <th>Location</th>
                  <th>Records</th>
                  <th>Size</th>
                  <th>Created</th>
                  <th>Verified</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let inst of ds.instances">
                  <td><span class="badge" style="background: #4f46e522; color: #4f46e5;">{{ inst.storageType }}</span></td>
                  <td>{{ inst.format }}</td>
                  <td>
                    <span class="badge"
                          [style.background]="inst.status === 'AVAILABLE' ? '#16a34a22' : '#ea580c22'"
                          [style.color]="inst.status === 'AVAILABLE' ? '#16a34a' : '#ea580c'">
                      {{ inst.status }}
                    </span>
                  </td>
                  <td class="small text-muted">{{ inst.storageLocationHint || '—' }}</td>
                  <td>{{ inst.loadedRecordCount | number }}</td>
                  <td>{{ inst.fileSizeBytes ? (inst.fileSizeBytes / 1024 / 1024 | number:'1.1-1') + ' MB' : '—' }}</td>
                  <td class="small">{{ inst.createdAt | date:'short' }}</td>
                  <td class="small">{{ inst.lastVerifiedAt | date:'short' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DatasetsComponent implements OnInit {
  datasets: DatasetDetails[] = [];
  loading = true;
  seeding = false;
  seedMessage = '';

  constructor(private datasetService: DatasetService) {}

  ngOnInit() {
    console.log('[DatasetsComponent] ngOnInit called');
    this.loadDatasets();
  }

  loadDatasets() {
    this.loading = true;
    console.log('[DatasetsComponent] calling getAllDatasets...');
    this.datasetService.getAllDatasets().subscribe({
      next: (data) => {
        console.log('[DatasetsComponent] SUCCESS, data:', data);
        this.datasets = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('[DatasetsComponent] ERROR:', err);
        this.loading = false;
      }
    });
  }

  seedDefaults() {
    this.seeding = true;
    this.datasetService.seedDefaults().subscribe({
      next: () => {
        this.seedMessage = 'Default datasets registered successfully!';
        this.seeding = false;
        this.loadDatasets();
      },
      error: () => { this.seeding = false; }
    });
  }
}
