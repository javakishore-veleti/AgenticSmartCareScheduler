import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SecuritySetting {
  id: number;
  settingName: string;
  settingType: string;
  configsJson: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class SecuritySettingsService {
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/security-settings';

  constructor(private http: HttpClient) {}

  getAll(): Observable<SecuritySetting[]> {
    return this.http.get<SecuritySetting[]>(this.baseUrl);
  }

  getByType(type: string): Observable<SecuritySetting[]> {
    return this.http.get<SecuritySetting[]>(`${this.baseUrl}/type/${type}`);
  }

  create(setting: any): Observable<SecuritySetting> {
    return this.http.post<SecuritySetting>(this.baseUrl, setting);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }
}
