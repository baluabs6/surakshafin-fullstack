import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, GrievanceView } from '../../core/models';

export interface RoutingRequest {
  issueType: string;
  description: string;
  alreadyRaisedWithBank: boolean;
  bankResponseOverThirtyDays: boolean;
  suspectedFraud: boolean;
}

@Injectable({ providedIn: 'root' })
export class GrievanceApiService {
  constructor(private http: HttpClient) {}

  file(req: RoutingRequest): Observable<GrievanceView> {
    return this.http.post<ApiResponse<GrievanceView>>('/api/v1/grievances', req).pipe(map((r) => r.data));
  }

  mine(): Observable<GrievanceView[]> {
    return this.http.get<ApiResponse<GrievanceView[]>>('/api/v1/grievances/mine').pipe(map((r) => r.data));
  }
}
