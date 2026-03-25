import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dataset-instance-view',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="mb-4">
      <a routerLink="/dataset-instances" class="text-decoration-none" style="color: #7c3aed;">
        <i class="bi bi-arrow-left me-1"></i>Back to Instances
      </a>
    </div>

    <h2 class="fw-bold mb-4" style="color: #7c3aed;">
      <i class="bi bi-hdd-stack me-2"></i>Dataset Instance Details
    </h2>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #7c3aed;"></div>
    </div>

    <div *ngIf="!loading && instance" class="card">
      <div class="card-body">
        <div class="row g-4">
          <div class="col-md-6">
            <label class="form-label small fw-semibold text-muted">Instance Name</label>
            <input type="text" class="form-control" [(ngModel)]="instance.instanceName" [disabled]="!editing">
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold text-muted">Status</label>
            <div>
              <span class="badge fs-6"
                    [style.background]="instance.status === 'AVAILABLE' ? '#16a34a22' : '#ea580c22'"
                    [style.color]="instance.status === 'AVAILABLE' ? '#16a34a' : '#ea580c'">
                {{ instance.status }}
              </span>
            </div>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold text-muted">Storage Type</label>
            <div><span class="badge fs-6" style="background: #4f46e522; color: #4f46e5;">{{ instance.storageType }}</span></div>
          </div>
        </div>

        <hr class="my-4">

        <div class="row g-4">
          <div class="col-md-4">
            <label class="form-label small fw-semibold text-muted">Dataset</label>
            <p class="fw-semibold" style="color: #0d9488;">{{ instance.datasetName }}</p>
          </div>
          <div class="col-md-4">
            <label class="form-label small fw-semibold text-muted">Format</label>
            <p>{{ instance.format }}</p>
          </div>
          <div class="col-md-4">
            <label class="form-label small fw-semibold text-muted">Records Loaded</label>
            <p class="fw-bold" style="color: #4f46e5;">{{ instance.loadedRecordCount | number }}</p>
          </div>
        </div>

        <div class="row g-4">
          <div class="col-md-6">
            <label class="form-label small fw-semibold text-muted">Storage Location</label>
            <p class="text-muted small">{{ instance.storageLocationHint }}</p>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold text-muted">File Size</label>
            <p>{{ instance.fileSizeBytes ? (instance.fileSizeBytes / 1024 / 1024 | number:'1.1-1') + ' MB' : '—' }}</p>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold text-muted">Created</label>
            <p class="small">{{ instance.createdAt | date:'medium' }}</p>
          </div>
        </div>

        <div class="mt-4">
          <button *ngIf="!editing" class="btn me-2" style="background: #7c3aed; color: white; border-radius: 10px;" (click)="editing = true">
            <i class="bi bi-pencil me-1"></i>Edit
          </button>
          <button *ngIf="!editing" class="btn me-2 btn-outline-danger" style="border-radius: 10px;" (click)="deleteInstance()">
            <i class="bi bi-trash me-1"></i>Delete
          </button>
          <a *ngIf="!editing" routerLink="/dataset-instances" class="btn btn-outline-secondary" style="border-radius: 10px;">
            <i class="bi bi-arrow-left me-1"></i>Back
          </a>
          <button *ngIf="editing" class="btn me-2" style="background: #0d9488; color: white; border-radius: 10px;" (click)="save()" [disabled]="saving">
            <i class="bi bi-save me-1"></i>{{ saving ? 'Saving...' : 'Save' }}
          </button>
          <button *ngIf="editing" class="btn btn-outline-secondary" style="border-radius: 10px;" (click)="editing = false">Cancel Edit</button>
        </div>

        <div *ngIf="successMsg" class="alert alert-success mt-3">
          <i class="bi bi-check-circle me-1"></i>{{ successMsg }}
        </div>
      </div>
    </div>
  `
})
export class DatasetInstanceViewComponent implements OnInit {
  instance: any = null;
  loading = true;
  editing = false;
  saving = false;
  successMsg = '';
  private instanceId!: number;
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/analytics/datasets';

  constructor(private route: ActivatedRoute, private routerNav: Router, private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.instanceId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadInstance();
  }

  loadInstance() {
    this.loading = true;
    // Load all datasets and find this instance
    this.http.get<any[]>(this.baseUrl).subscribe({
      next: (datasets) => {
        for (const ds of datasets) {
          const found = ds.instances.find((i: any) => i.instanceId === this.instanceId);
          if (found) {
            this.instance = { ...found, datasetName: ds.displayName, datasetCode: ds.datasetCode };
            break;
          }
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  save() {
    this.saving = true;
    this.http.put(`${this.baseUrl}/instances/${this.instanceId}`, { instanceName: this.instance.instanceName }).subscribe({
      next: () => {
        this.saving = false;
        this.editing = false;
        this.successMsg = 'Instance name updated!';
        this.cdr.detectChanges();
      },
      error: () => { this.saving = false; this.cdr.detectChanges(); }
    });
  }

  deleteInstance() {
    if (!confirm('Are you sure you want to delete this instance?')) return;
    this.http.delete(`${this.baseUrl}/instances/${this.instanceId}`).subscribe({
      next: () => { this.routerNav.navigate(['/dataset-instances']); },
      error: (err) => { console.error('Delete failed', err); }
    });
  }
}
