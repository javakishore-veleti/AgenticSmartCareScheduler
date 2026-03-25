import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DatasetService, DatasetDetails } from '../../services/dataset.service';

@Component({
  selector: 'app-dataset-instances',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2 class="fw-bold mb-4" style="color: #7c3aed;">
      <i class="bi bi-hdd-stack me-2"></i>Dataset Instances
    </h2>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #7c3aed;" role="status"></div>
      <p class="mt-2 text-muted">Loading instances...</p>
    </div>

    <div *ngIf="!loading && allInstances.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-hdd-stack fs-1" style="color: #ea580c;"></i>
        <h5 class="mt-3 fw-bold">No Dataset Instances</h5>
        <p class="text-muted">Go to <a routerLink="/datasets" style="color: #4f46e5;">Dataset Definitions</a> and ingest a dataset first.</p>
      </div>
    </div>

    <div class="table-responsive" *ngIf="!loading && allInstances.length > 0">
      <table class="table table-hover align-middle">
        <thead>
          <tr style="color: #7c3aed;">
            <th>Instance Name</th>
            <th>Dataset</th>
            <th>Storage</th>
            <th>Status</th>
            <th>Location</th>
            <th>Records</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let inst of pagedInstances">
            <td class="fw-semibold" style="color: #7c3aed;">{{ inst.instanceName || 'Unnamed' }}</td>
            <td>
              <a [routerLink]="['/datasets']" style="color: #4f46e5; text-decoration: none;">
                {{ inst.datasetName }}
              </a>
            </td>
            <td><span class="badge" style="background: #4f46e522; color: #4f46e5;">{{ inst.storageType }}</span></td>
            <td>
              <span class="badge"
                    [style.background]="inst.status === 'AVAILABLE' ? '#16a34a22' : '#ea580c22'"
                    [style.color]="inst.status === 'AVAILABLE' ? '#16a34a' : '#ea580c'">
                {{ inst.status }}
              </span>
            </td>
            <td class="small text-muted" style="max-width: 250px; overflow: hidden; text-overflow: ellipsis;">{{ inst.storageLocationHint }}</td>
            <td>{{ inst.loadedRecordCount | number }}</td>
            <td class="small">{{ inst.createdAt | date:'short' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <nav *ngIf="totalPages > 1">
        <ul class="pagination pagination-sm justify-content-center">
          <li class="page-item" [class.disabled]="currentPage === 1">
            <a class="page-link" (click)="goToPage(currentPage - 1)" style="color: #7c3aed;">Previous</a>
          </li>
          <li class="page-item" *ngFor="let p of pages" [class.active]="p === currentPage">
            <a class="page-link" (click)="goToPage(p)" [style.background]="p === currentPage ? '#7c3aed' : ''" [style.color]="p === currentPage ? 'white' : '#7c3aed'">{{ p }}</a>
          </li>
          <li class="page-item" [class.disabled]="currentPage === totalPages">
            <a class="page-link" (click)="goToPage(currentPage + 1)" style="color: #7c3aed;">Next</a>
          </li>
        </ul>
      </nav>
    </div>
  `
})
export class DatasetInstancesComponent implements OnInit {
  allInstances: any[] = [];
  pagedInstances: any[] = [];
  loading = true;
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  pages: number[] = [];

  constructor(private datasetService: DatasetService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadInstances();
  }

  loadInstances() {
    this.loading = true;
    this.datasetService.getAllDatasets().subscribe({
      next: (datasets) => {
        this.allInstances = [];
        datasets.forEach(ds => {
          ds.instances.forEach(inst => {
            this.allInstances.push({ ...inst, datasetName: ds.displayName, datasetCode: ds.datasetCode });
          });
        });
        this.totalPages = Math.max(1, Math.ceil(this.allInstances.length / this.pageSize));
        this.pages = Array.from({ length: this.totalPages }, (_, i) => i + 1);
        this.goToPage(1);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  goToPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    const start = (page - 1) * this.pageSize;
    this.pagedInstances = this.allInstances.slice(start, start + this.pageSize);
    this.cdr.detectChanges();
  }
}
