import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-messages-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mb-4">
      <a routerLink="/messages" class="text-decoration-none" style="color: #ea580c;">
        <i class="bi bi-arrow-left me-1"></i>Back to Topics
      </a>
    </div>

    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #ea580c;">
        <i class="bi bi-collection me-2"></i>{{ topicName }}
      </h2>
      <button class="btn btn-sm" style="background: #ea580c22; color: #ea580c; border-radius: 8px;" (click)="loadMessages()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #ea580c;"></div>
    </div>

    <div class="table-responsive" *ngIf="!loading">
      <table class="table table-hover align-middle">
        <thead>
          <tr style="color: #ea580c;">
            <th>Seq</th>
            <th>Key</th>
            <th>Status</th>
            <th>Payload</th>
            <th>Context</th>
            <th>Created</th>
            <th>Processed</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let m of pagedMessages">
            <td class="fw-semibold">{{ m.sequenceNumber }}</td>
            <td><span class="badge" style="background: #4f46e522; color: #4f46e5;">{{ m.messageKey || '—' }}</span></td>
            <td>
              <span class="badge"
                    [style.background]="statusColor(m.status).bg"
                    [style.color]="statusColor(m.status).fg">
                {{ m.status }}
              </span>
            </td>
            <td class="small text-muted" style="max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
              {{ m.payload | slice:0:80 }}{{ m.payload?.length > 80 ? '...' : '' }}
            </td>
            <td class="small text-muted">{{ formatCtx(m.ctxData) }}</td>
            <td class="small">{{ m.createdAt | date:'short' }}</td>
            <td class="small">{{ m.processedAt ? (m.processedAt | date:'short') : '—' }}</td>
          </tr>
        </tbody>
      </table>

      <nav *ngIf="totalPages > 1">
        <ul class="pagination pagination-sm justify-content-center">
          <li class="page-item" [class.disabled]="currentPage === 1">
            <a class="page-link" (click)="goToPage(currentPage - 1)" style="color: #ea580c;">Prev</a>
          </li>
          <li class="page-item" *ngFor="let p of pages" [class.active]="p === currentPage">
            <a class="page-link" (click)="goToPage(p)"
               [style.background]="p === currentPage ? '#ea580c' : ''" [style.color]="p === currentPage ? 'white' : '#ea580c'">{{ p }}</a>
          </li>
          <li class="page-item" [class.disabled]="currentPage === totalPages">
            <a class="page-link" (click)="goToPage(currentPage + 1)" style="color: #ea580c;">Next</a>
          </li>
        </ul>
      </nav>
    </div>
  `
})
export class MessagesDetailComponent implements OnInit {
  topicName = '';
  allMessages: any[] = [];
  pagedMessages: any[] = [];
  loading = true;
  currentPage = 1;
  pageSize = 20;
  totalPages = 1;
  pages: number[] = [];
  private brokerUrl = 'http://localhost:8081/smart-care/api/broker/v1/messages';

  constructor(private route: ActivatedRoute, private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.topicName = this.route.snapshot.paramMap.get('topicName') || '';
    this.loadMessages();
  }

  loadMessages() {
    this.loading = true;
    this.http.get<any[]>(`${this.brokerUrl}/topic/${this.topicName}`).subscribe({
      next: (data) => {
        this.allMessages = data;
        this.totalPages = Math.max(1, Math.ceil(data.length / this.pageSize));
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
    this.pagedMessages = this.allMessages.slice(start, start + this.pageSize);
    this.cdr.detectChanges();
  }

  statusColor(status: string) {
    switch (status) {
      case 'PENDING': return { bg: '#fef3c7', fg: '#d97706' };
      case 'PROCESSING': return { bg: '#dbeafe', fg: '#2563eb' };
      case 'COMPLETED': return { bg: '#d1fae5', fg: '#059669' };
      case 'FAILED': return { bg: '#fee2e2', fg: '#dc2626' };
      default: return { bg: '#f3f4f6', fg: '#6b7280' };
    }
  }

  formatCtx(ctx: any): string {
    if (!ctx) return '—';
    return Object.entries(ctx).map(([k, v]) => `${k}=${v}`).join(', ').substring(0, 60);
  }
}
