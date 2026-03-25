import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SecuritySettingsService, SecuritySetting } from '../../services/security-settings.service';
import { AWS_REGIONS } from '../../services/aws-constants';

@Component({
  selector: 'app-secrets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2 class="fw-bold mb-4" style="color: #e11d48;">
      <i class="bi bi-shield-lock me-2"></i>Security Settings
    </h2>

    <!-- Create New -->
    <div class="card mb-4">
      <div class="card-header bg-white border-0 pt-3">
        <h5 class="fw-bold"><i class="bi bi-plus-circle me-2" style="color: #e11d48;"></i>Create New Secret</h5>
      </div>
      <div class="card-body">
        <div class="row g-3 align-items-end">
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Name</label>
            <input type="text" class="form-control form-control-sm" [(ngModel)]="newSetting.settingName" placeholder="my-aws-profile">
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Secret Type</label>
            <select class="form-select form-select-sm" [(ngModel)]="newSetting.settingType" (change)="onTypeChange()">
              <option value="AWS_CLIENT_PROFILE">AWS Client Profile</option>
              <option value="AWS_CLIENT_CREDENTIALS">AWS Client Credentials</option>
              <option value="AWS_SECRETS_MANAGER">AWS Secrets Manager (Coming Soon)</option>
            </select>
          </div>

          <!-- AWS_CLIENT_PROFILE fields -->
          <ng-container *ngIf="newSetting.settingType === 'AWS_CLIENT_PROFILE'">
            <div class="col-md-2">
              <label class="form-label small fw-semibold">Profile</label>
              <div class="input-group input-group-sm">
                <select *ngIf="localProfiles.length > 0" class="form-select form-select-sm" [(ngModel)]="configFields.profileName"
                        (change)="onProfileSelect()">
                  <option *ngFor="let p of localProfiles" [value]="p.profileName">
                    {{ p.profileName }} {{ p.region ? '(' + p.region + ')' : '' }}
                  </option>
                </select>
                <input *ngIf="localProfiles.length === 0" type="text" class="form-control form-control-sm"
                       [(ngModel)]="configFields.profileName" placeholder="default">
              </div>
            </div>
            <div class="col-md-2">
              <label class="form-label small fw-semibold">&nbsp;</label>
              <button class="btn btn-sm w-100 d-block" style="background: #f0fdfa; color: #0d9488; border: 1px solid #0d9488; border-radius: 8px;"
                      (click)="scanProfiles()" [disabled]="scanning">
                <i class="bi bi-search me-1"></i>{{ scanning ? 'Scanning...' : 'Scan Local Profiles' }}
              </button>
            </div>
          </ng-container>

          <!-- AWS_CLIENT_CREDENTIALS fields -->
          <ng-container *ngIf="newSetting.settingType === 'AWS_CLIENT_CREDENTIALS'">
            <div class="col-md-2">
              <label class="form-label small fw-semibold">Access Key ID</label>
              <input type="text" class="form-control form-control-sm" [(ngModel)]="configFields.accessKeyId" placeholder="AKIA...">
            </div>
            <div class="col-md-2">
              <label class="form-label small fw-semibold">Secret Key</label>
              <input type="password" class="form-control form-control-sm" [(ngModel)]="configFields.secretAccessKey" placeholder="****">
            </div>
            <div class="col-md-2">
              <label class="form-label small fw-semibold">Region</label>
              <select class="form-select form-select-sm" [(ngModel)]="configFields.region">
                <option *ngFor="let r of awsRegions" [value]="r.code">{{ r.name }} ({{ r.code }})</option>
              </select>
            </div>
          </ng-container>

          <!-- AWS_SECRETS_MANAGER -->
          <ng-container *ngIf="newSetting.settingType === 'AWS_SECRETS_MANAGER'">
            <div class="col-md-4">
              <span class="badge rounded-pill" style="background: #fef3c7; color: #d97706;">
                <i class="bi bi-clock me-1"></i>Coming Soon
              </span>
            </div>
          </ng-container>

          <div class="col-md-2" *ngIf="newSetting.settingType !== 'AWS_SECRETS_MANAGER'">
            <button class="btn btn-sm w-100" style="background: #e11d48; color: white; border-radius: 8px;"
                    (click)="createSetting()" [disabled]="creating">
              <i class="bi bi-save me-1"></i>{{ creating ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- List -->
    <div *ngIf="loading" class="text-center py-3">
      <div class="spinner-border spinner-border-sm" style="color: #e11d48;"></div>
    </div>

    <div *ngIf="!loading && settings.length === 0" class="card">
      <div class="card-body text-center py-4">
        <i class="bi bi-shield-x fs-2" style="color: #ea580c;"></i>
        <p class="mt-2 text-muted">No security settings configured yet.</p>
      </div>
    </div>

    <div class="table-responsive" *ngIf="!loading && settings.length > 0">
      <table class="table table-hover align-middle">
        <thead>
          <tr style="color: #e11d48;">
            <th>Name</th>
            <th>Type</th>
            <th>Config</th>
            <th>Created</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let s of settings">
            <td class="fw-semibold">{{ s.settingName }}</td>
            <td><span class="badge" style="background: #e11d4822; color: #e11d48;">{{ s.settingType }}</span></td>
            <td class="small text-muted" style="max-width: 300px; overflow: hidden; text-overflow: ellipsis;">{{ maskConfig(s.configsJson) }}</td>
            <td class="small">{{ s.createdAt | date:'short' }}</td>
            <td>
              <button class="btn btn-sm btn-outline-danger" (click)="deleteSetting(s.id)">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class SecretsComponent implements OnInit {
  settings: SecuritySetting[] = [];
  loading = true;
  creating = false;
  scanning = false;
  awsRegions = AWS_REGIONS;
  localProfiles: { profileName: string; region: string }[] = [];
  nameAutoFilled = false;
  newSetting = { settingName: '', settingType: 'AWS_CLIENT_PROFILE' };
  configFields: any = { profileName: 'default', region: 'us-east-1', accessKeyId: '', secretAccessKey: '' };

  constructor(private service: SecuritySettingsService, private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() { this.loadSettings(); }

  onTypeChange() {
    this.localProfiles = [];
    this.cdr.detectChanges();
  }

  scanProfiles() {
    this.scanning = true;
    this.cdr.detectChanges();
    this.http.get<{ profileName: string; region: string }[]>(
      window.location.origin + '/smart-care/api/admin/v1/aws-profiles/scan'
    ).subscribe({
      next: (profiles) => {
        this.localProfiles = profiles;
        this.scanning = false;
        if (profiles.length > 0) {
          this.configFields.profileName = profiles[0].profileName;
          this.configFields.region = profiles[0].region;
          this.onProfileSelect();
        }
        this.cdr.detectChanges();
      },
      error: () => { this.scanning = false; this.cdr.detectChanges(); }
    });
  }

  onProfileSelect() {
    const p = this.localProfiles.find(x => x.profileName === this.configFields.profileName);
    if (p) {
      this.configFields.region = p.region;
      if (!this.newSetting.settingName || this.nameAutoFilled) {
        this.newSetting.settingName = 'aws-profile-' + p.profileName;
        this.nameAutoFilled = true;
      }
    }
    this.cdr.detectChanges();
  }

  loadSettings() {
    this.loading = true;
    this.service.getAll().subscribe({
      next: (data) => { this.settings = data; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
  }

  createSetting() {
    this.creating = true;
    const entity: any = {
      settingName: this.newSetting.settingName,
      settingType: this.newSetting.settingType,
      configsJson: JSON.stringify(this.configFields)
    };
    this.service.create(entity).subscribe({
      next: () => {
        this.creating = false;
        this.newSetting.settingName = '';
        this.loadSettings();
      },
      error: () => { this.creating = false; this.cdr.detectChanges(); }
    });
  }

  deleteSetting(id: number) {
    this.service.delete(id).subscribe({ next: () => this.loadSettings() });
  }

  maskConfig(json: string): string {
    try {
      const obj = JSON.parse(json);
      if (obj.secretAccessKey) obj.secretAccessKey = '****';
      return JSON.stringify(obj);
    } catch { return json; }
  }
}
