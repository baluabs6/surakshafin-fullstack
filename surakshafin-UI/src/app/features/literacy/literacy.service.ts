import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, LiteracyContentView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class LiteracyService {
  constructor(private http: HttpClient) {}

  list(language: string, topic?: string): Observable<LiteracyContentView[]> {
    const params: Record<string, string> = { language };
    if (topic) params['topic'] = topic;
    return this.http
      .get<ApiResponse<LiteracyContentView[]>>('/api/v1/literacy', { params })
      .pipe(map((r) => r.data));
  }
}
