import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SecuritySettingsService, SecuritySetting } from '../../services/security-settings.service';
import { AWS_REGIONS } from '../../services/aws-constants';

@Component({
  selector: 'app-secret-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="mb-4">
      <a routerLink="/secrets" class="text-decoration-none" style="color: #e11d48;">
        <i class="bi bi-arrow-left me-1"></i>Back to Secrets
      </a>
    </div>

    <h2 class="fw-bold mb-4" style="color: #e11d48;">
      <i class="bi bi-pencil-square me-2"></i>Edit Security Setting
    </h2>

    <div *ngIf="loading" class="text-center py-5">
      <div class="spinner-border" style="color: #e11d48;"></div>
    </div>

    <div *ngIf="!loading" class="card">
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label small fw-semibold">Name</label>
            <input type="text" class="form-control" [(ngModel)]="settingName">
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Type</label>
            <input type="text" class="form-control" [value]="settingType" disabled>
          </div>
        </div>

        <hr class="my-4">
        <h6 class="fw-bold" style="color: #4f46e5;">Update Credentials</h6>
        <p class="text-muted small">Leave blank to keep existing values.</p>

        <!-- AWS_CLIENT_PROFILE -->
        <div class="row g-3" *ngIf="settingType === 'AWS_CLIENT_PROFILE'">
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Profile Name</label>
            <div class="input-group">
              <select *ngIf="localProfiles.length > 0" class="form-select" [(ngModel)]="configFields.profileName">
                <option *ngFor="let p of localProfiles" [value]="p.profileName">
                  {{ p.profileName }} {{ p.region ? '(' + p.region + ')' : '' }}
                </option>
              </select>
              <input *ngIf="localProfiles.length === 0" type="text" class="form-control" [(ngModel)]="configFields.profileName">
            </div>
          </div>
          <div class="col-md-2">
            <label class="form-label small fw-semibold">&nbsp;</label>
            <button class="btn btn-sm d-block w-100" style="background: #f0fdfa; color: #0d9488; border: 1px solid #0d9488;"
                    (click)="scanProfiles()" [disabled]="scanning">
              <i class="bi bi-search me-1"></i>{{ scanning ? 'Scanning...' : 'Scan Profiles' }}
            </button>
          </div>
        </div>

        <!-- AWS_CLIENT_CREDENTIALS -->
        <div class="row g-3" *ngIf="settingType === 'AWS_CLIENT_CREDENTIALS'">
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Access Key ID</label>
            <input type="text" class="form-control" [(ngModel)]="configFields.accessKeyId" placeholder="Leave blank to keep existing">
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Secret Access Key</label>
            <input type="password" class="form-control" [(ngModel)]="configFields.secretAccessKey" placeholder="Leave blank to keep existing">
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Region</label>
            <select class="form-select" [(ngModel)]="configFields.region">
              <option *ngFor="let r of awsRegions" [value]="r.code">{{ r.name }} ({{ r.code }})</option>
            </select>
          </div>
        </div>

        <!-- AWS_SECRETS_MANAGER -->
        <div *ngIf="settingType === 'AWS_SECRETS_MANAGER'">
          <span class="badge rounded-pill" style="background: #fef3c7; color: #d97706;">
            <i class="bi bi-clock me-1"></i>Coming Soon
          </span>
        </div>

        <div class="mt-4">
          <button class="btn me-2" style="background: #e11d48; color: white; border-radius: 10px;"
                  (click)="save()" [disabled]="saving">
            <i class="bi bi-save me-1"></i>{{ saving ? 'Saving...' : 'Save Changes' }}
          </button>
          <a routerLink="/secrets" class="btn btn-outline-secondary" style="border-radius: 10px;">Cancel</a>
        </div>

        <div *ngIf="successMsg" class="alert alert-success mt-3">
          <i class="bi bi-check-circle me-1"></i>{{ successMsg }}
        </div>
      </div>
    </div>
  `
})
export class SecretEditComponent implements OnInit {
  settingId!: number;
  settingName = '';
  settingType = '';
  loading = true;
  saving = false;
  scanning = false;
  successMsg = '';
  awsRegions = AWS_REGIONS;
  localProfiles: { profileName: string; region: string }[] = [];
  configFields: any = { profileName: '', region: 'us-east-1', accessKeyId: '', secretAccessKey: '' };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: SecuritySettingsService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.settingId = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getEditInfo(this.settingId).subscribe({
      next: (data) => {
        this.settingName = data.settingName;
        this.settingType = data.settingType;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  scanProfiles() {
    this.scanning = true;
    this.http.get<any[]>(window.location.origin + '/smart-care/api/admin/v1/aws-profiles/scan').subscribe({
      next: (profiles) => { this.localProfiles = profiles; this.scanning = false; this.cdr.detectChanges(); },
      error: () => { this.scanning = false; this.cdr.detectChanges(); }
    });
  }

  save() {
    this.saving = true;
    const config: any = {};
    if (this.settingType === 'AWS_CLIENT_PROFILE') {
      if (this.configFields.profileName) config.profileName = this.configFields.profileName;
      const p = this.localProfiles.find(x => x.profileName === this.configFields.profileName);
      if (p?.region) config.region = p.region;
    } else if (this.settingType === 'AWS_CLIENT_CREDENTIALS') {
      if (this.configFields.accessKeyId) config.accessKeyId = this.configFields.accessKeyId;
      if (this.configFields.secretAccessKey) config.secretAccessKey = this.configFields.secretAccessKey;
      if (this.configFields.region) config.region = this.configFields.region;
    }

    const body: any = {
      settingName: this.settingName,
      settingType: this.settingType,
      configsJson: Object.keys(config).length > 0 ? JSON.stringify(config) : null
    };

    this.service.update(this.settingId, body).subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = 'Settings updated successfully!';
        this.cdr.detectChanges();
      },
      error: () => { this.saving = false; this.cdr.detectChanges(); }
    });
  }
}
