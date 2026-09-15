import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, FraudReportView, ScamPattern } from '../../core/models';

export interface ReportRequest {
  category: string;
  details: string;
  suspectUpiId?: string;
  suspectPhoneNumber?: string;
}

@Injectable({ providedIn: 'root' })
export class FraudApiService {
  constructor(private http: HttpClient) {}

  patterns(): Observable<ScamPattern[]> {
    return this.http.get<ApiResponse<ScamPattern[]>>('/api/v1/fraud/patterns').pipe(map((r) => r.data));
  }

  submitReport(req: ReportRequest): Observable<FraudReportView> {
    return this.http.post<ApiResponse<FraudReportView>>('/api/v1/fraud/reports', req).pipe(map((r) => r.data));
  }

  myReports(): Observable<FraudReportView[]> {
    return this.http.get<ApiResponse<FraudReportView[]>>('/api/v1/fraud/reports/mine').pipe(map((r) => r.data));
  }
}
