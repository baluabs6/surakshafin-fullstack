import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse, AuthResponse, UserProfile } from './models';

const TOKEN_KEY = 'surakshafin_token';
const PROFILE_KEY = 'surakshafin_profile';

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Signal so nav/guards react immediately to login/logout without a page reload. */
  readonly currentUser = signal<UserProfile | null>(this.readStoredProfile());

  constructor(private http: HttpClient) {}

  register(payload: { phoneNumber: string; fullName: string; password: string; preferredLanguage: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/register', payload).pipe(
      tap((res) => this.storeSession(res.data))
    );
  }

  login(payload: { phoneNumber: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', payload).pipe(
      tap((res) => this.storeSession(res.data))
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(PROFILE_KEY);
    this.currentUser.set(null);
  }

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.token;
  }

  private storeSession(auth: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, auth.token);
    localStorage.setItem(PROFILE_KEY, JSON.stringify(auth.profile));
    this.currentUser.set(auth.profile);
  }

  private readStoredProfile(): UserProfile | null {
    const raw = localStorage.getItem(PROFILE_KEY);
    return raw ? (JSON.parse(raw) as UserProfile) : null;
  }
}
