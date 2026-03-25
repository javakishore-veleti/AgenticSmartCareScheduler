import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-messages-topics',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold" style="color: #ea580c;">
        <i class="bi bi-envelope-paper me-2"></i>Message Topics
      </h2>
      <button class="btn btn-sm" style="background: #ea580c22; color: #ea580c; border-radius: 8px;" (click)="loadTopics()">
        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
      </button>
    </div>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #ea580c;"></div>
    </div>

    <div *ngIf="!loading && topics.length === 0" class="card">
      <div class="card-body text-center py-5">
        <i class="bi bi-inbox fs-1" style="color: #ea580c; opacity: 0.5;"></i>
        <h5 class="mt-3 fw-bold">No Topics</h5>
        <p class="text-muted">No messages have been published yet. Topics appear automatically when messages are published.</p>
      </div>
    </div>

    <div class="row g-3" *ngIf="!loading && topics.length > 0">
      <div class="col-md-6 col-lg-4" *ngFor="let t of topics">
        <div class="card h-100" style="border-left: 4px solid #ea580c;">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h6 class="fw-bold mb-0" style="color: #ea580c;">
                <i class="bi bi-collection me-1"></i>{{ t.topicName }}
              </h6>
              <span class="badge rounded-pill" style="background: #4f46e522; color: #4f46e5;">{{ t.total }} total</span>
            </div>
            <div class="d-flex gap-2 mb-3">
              <span class="badge" style="background: #fef3c7; color: #d97706;">{{ t.pending }} pending</span>
              <span class="badge" style="background: #dbeafe; color: #2563eb;">{{ t.processing }} processing</span>
              <span class="badge" style="background: #d1fae5; color: #059669;">{{ t.completed }} done</span>
              <span *ngIf="t.failed > 0" class="badge" style="background: #fee2e2; color: #dc2626;">{{ t.failed }} failed</span>
            </div>
            <a [routerLink]="['/messages', t.topicName]" class="btn btn-sm w-100" style="background: #ea580c; color: white; border-radius: 8px;">
              <i class="bi bi-list-ul me-1"></i>View Messages
            </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class MessagesTopicsComponent implements OnInit {
  topics: any[] = [];
  loading = true;
  private brokerUrl = 'http://localhost:8081/smart-care/api/broker/v1/messages';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadTopics(); }

  loadTopics() {
    this.loading = true;
    this.http.get<any[]>(`${this.brokerUrl}/topics`).subscribe({
      next: (data) => { this.topics = data; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.topics = []; this.loading = false; this.cdr.detectChanges(); }
    });
  }
}
