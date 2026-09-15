import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LiteracyService } from './literacy.service';
import { LiteracyContentView } from '../../core/models';

const LANGUAGES: Record<string, string> = {
  en: 'English', hi: 'हिन्दी', te: 'తెలుగు', ta: 'தமிழ்', bn: 'বাংলা', mr: 'मराठी',
};

const TOPIC_LABELS: Record<string, string> = {
  UPI_SAFETY: 'UPI safety',
  BNPL_RISKS: 'Buy-now-pay-later',
  MULE_ACCOUNTS: 'Mule accounts',
  GRIEVANCE_RIGHTS: 'Your rights',
};

@Component({
  selector: 'app-literacy',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Learn</h1>
    <p class="muted">Short, plain-language explainers — no jargon, no sign-in required.</p>

    <div class="field" style="max-width:260px;">
      <label for="lang">Language</label>
      <select id="lang" [(ngModel)]="language" (ngModelChange)="reload()">
        @for (code of languageCodes; track code) {
          <option [value]="code">{{ languages[code] }}</option>
        }
      </select>
    </div>

    @if (loading()) {
      <p class="muted">Loading…</p>
    } @else if (articles().length === 0) {
      <div class="empty-state">No articles yet in this language. Try English while more languages are added.</div>
    } @else {
      @for (a of articles(); track a.id) {
        <div class="ledger accent-success">
          <span class="badge badge-status">{{ topicLabel(a.topic) }}</span>
          <h3 style="margin-top:0.5rem;">{{ a.title }}</h3>
          <p>{{ a.body }}</p>
        </div>
      }
    }
  `,
})
export class LiteracyComponent implements OnInit {
  language = 'en';
  languages = LANGUAGES;
  languageCodes = Object.keys(LANGUAGES);
  articles = signal<LiteracyContentView[]>([]);
  loading = signal(true);

  constructor(private literacyService: LiteracyService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.literacyService.list(this.language).subscribe({
      next: (data) => {
        this.articles.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  topicLabel(topic: string): string {
    return TOPIC_LABELS[topic] ?? topic;
  }
}
