import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="top-bar">
      <div class="top-bar-inner">
        <a routerLink="/literacy" class="brand">
          SuRakshaFin
          <small>Fraud protection &amp; financial well-being</small>
        </a>
        <nav class="primary-nav">
          <a routerLink="/literacy" routerLinkActive="active">Learn</a>
          <a routerLink="/fraud" routerLinkActive="active">Fraud Protection</a>
          <a routerLink="/grievance" routerLinkActive="active">Grievances</a>
          <a routerLink="/budget" routerLinkActive="active">Spend Tracker</a>
        </nav>
        <div class="session-box">
          @if (auth.currentUser(); as user) {
            <span>{{ user.fullName }}</span>
            <button class="btn-quiet" (click)="logout()">Log out</button>
          } @else {
            <a routerLink="/login" class="btn btn-quiet" style="color:#FBF6E3;border-color:#3E5C55;">Log in</a>
            <a routerLink="/register" class="btn btn-primary">Register</a>
          }
        </div>
      </div>
    </header>
    <main class="app-shell">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {
  constructor(public auth: AuthService) {}

  logout(): void {
    this.auth.logout();
  }
}
