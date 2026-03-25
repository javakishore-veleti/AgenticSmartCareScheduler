import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DatasetInstanceInfo {
  instanceId: number;
  storageType: string;
  format: string;
  status: string;
  storageLocationHint: string;
  isMultiFile: boolean;
  hasSubfolders: boolean;
  fileSizeBytes: number;
  loadedRecordCount: number;
  createdAt: string;
  lastVerifiedAt: string;
  errorMessage: string;
}

export interface DatasetDetails {
  datasetCode: string;
  displayName: string;
  description: string;
  sourceUrl: string;
  sourceProvider: string;
  licenseType: string;
  recordCount: number;
  columnCount: number;
  defaultFormat: string;
  tags: string;
  exists: boolean;
  instances: DatasetInstanceInfo[];
}

@Injectable({ providedIn: 'root' })
export class DatasetService {
  private baseUrl = window.location.origin + '/smart-care/api/admin/v1/analytics/datasets';

  constructor(private http: HttpClient) {}

  getAllDatasets(): Observable<DatasetDetails[]> {
    return this.http.get<DatasetDetails[]>(this.baseUrl);
  }

  getDatasetDetails(datasetCode: string): Observable<DatasetDetails> {
    return this.http.get<DatasetDetails>(`${this.baseUrl}/${datasetCode}/getDetails`);
  }

  seedDefaults(): Observable<any> {
    return this.http.post(`${this.baseUrl}/seed-defaults`, {});
  }

  ingestDataset(datasetCode: string, config?: any): Observable<DatasetDetails> {
    return this.http.post<DatasetDetails>(`${this.baseUrl}/${datasetCode}/ingest`, config || {});
  }
}
