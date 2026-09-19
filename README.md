# SuRakshaFin — Backend

Spring Boot 3 modular monolith implementing the core of the SuRakshaFin architecture blueprint:
`user-identity`, `fraud-intel`, `grievance-router`, `spend-budget`, and `literacy-content`, as one
deployable app with strict package boundaries (each package only talks to another through its
`*Service` class — never reaches into another package's repository or entity). This makes it
straightforward to later extract any package into its own Spring Boot microservice, which is the
path the full blueprint (`transaction-monitor-service`, `notification-service`,
`audit-compliance-service`, event bus, API gateway, OAuth2/OIDC, AWS deployment) describes.

## Run it

Requires Java 17+ and Maven (or use the wrapper if you generate one with `mvn -N wrapper:wrapper`).

Set a JWT signing secret first — none is hardcoded in the repo:

```bash
export SURAKSHAFIN_JWT_SECRET=$(openssl rand -hex 32)
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/docs`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:surakshafin`, user `sa`, blank password)

Demo scam patterns and literacy articles are seeded automatically on startup — see `DemoDataSeeder`.

## API summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | none | Create account (phone + password) |
| POST | `/api/v1/auth/login` | none | Get a JWT |
| POST | `/api/v1/auth/logout` | JWT | Revoke the current token |
| POST | `/api/v1/auth/forgot-password` | none | Request a password-reset code |
| POST | `/api/v1/auth/reset-password` | none | Reset password with the code |
| GET | `/api/v1/users/me` | JWT | Current profile |
| POST | `/api/v1/users/me/kyc-lite` | JWT | Submit PAN for KYC-lite verification |
| GET | `/api/v1/fraud/patterns` | JWT | Scam pattern library |
| POST | `/api/v1/fraud/reports` | JWT | One-tap fraud report |
| GET | `/api/v1/fraud/reports/mine` | JWT | My submitted reports |
| PATCH | `/api/v1/fraud/reports/{id}/status` | JWT (admin) | Move a report through its review lifecycle |
| POST | `/api/v1/fraud/patterns/{id}/confirm` | JWT | Confirm a scam pattern is still active |
| POST | `/api/v1/fraud/pre-transaction-check` | JWT | Risk-check a payee before a transfer is confirmed |
| POST | `/api/v1/fraud/trusted-payees` | JWT | Mark a payee as trusted |
| GET | `/api/v1/fraud/trusted-payees` | JWT | List trusted payees |
| DELETE | `/api/v1/fraud/trusted-payees/{id}` | JWT | Remove a trusted payee |
| POST | `/api/v1/grievances` | JWT | Run the routing wizard, file + auto-draft a complaint |
| GET | `/api/v1/grievances/mine` | JWT | My grievances |
| GET | `/api/v1/grievances/{id}` | JWT | One grievance, incl. generated complaint text |
| PATCH | `/api/v1/grievances/{id}/status` | JWT (admin) | Move a grievance through its lifecycle |
| POST | `/api/v1/budget/transactions` | JWT | Log a spend transaction |
| GET | `/api/v1/budget/transactions` | JWT | List transactions |
| PUT | `/api/v1/budget/limit` | JWT | Set monthly budget |
| GET | `/api/v1/budget/summary` | JWT | Spend vs. budget, category breakdown, BNPL exposure, nudge |
| GET | `/api/v1/literacy` | none | Vernacular literacy content (`?language=hi&topic=UPI_SAFETY`) |

## Security & feature updates in this revision

A security/feature-gap review added the following. Nothing here required a new dependency.

**Security fixes**
- `SURAKSHAFIN_JWT_SECRET` is now mandatory with no weak fallback — the app refuses to start
  without a real, 32-byte-plus secret (previously fell back to a fixed placeholder string).
- Passwords must now be 8+ characters with a letter and a number (previously any non-blank string).
- Login lockout after `surakshafin.security.max-login-attempts` (default 5) failed attempts, for
  `lockout-minutes` (default 15) — previously unlimited attempts were allowed.
- `/auth/logout` now actually revokes the bearer token via an in-memory blocklist — previously
  there was no way to end a session server-side.
- Unhandled exceptions no longer leak internal messages to the client; they're logged server-side
  and a generic message is returned instead.
- H2 console and Swagger/OpenAPI are now off by default; opt in for local dev with
  `SURAKSHAFIN_DEV_TOOLS_ENABLED=true`.
- CORS allowed origins are now configurable via `SURAKSHAFIN_CORS_ORIGINS` instead of hardcoded to
  `localhost`.
- Added `Strict-Transport-Security`, `X-Content-Type-Options`, and `Referrer-Policy` response headers.
- Free-text fields (fraud report details, grievance description, merchant name) now have request-level
  size caps, matching the DB column limits.

**Feature gaps closed**
- `User.kycLiteVerified` and both `FraudReport.status` / `Grievance.status` fields existed but had no
  code path that ever changed them. Added `POST /users/me/kyc-lite`,
  `PATCH /fraud/reports/{id}/status`, `PATCH /grievances/{id}/status` (the latter two are
  operator/admin-only — see below).
- Added `POST /auth/forgot-password` and `POST /auth/reset-password` — there was previously no
  account-recovery path. (Demo-mode: the reset code is returned in the API response since no SMS
  gateway is wired up; production would text it and return only an acknowledgement.)
- Added a minimal admin/operator role (`User.admin`), bootstrapped only via
  `SURAKSHAFIN_ADMIN_PHONE` / `SURAKSHAFIN_ADMIN_PASSWORD` env vars at first startup — needed so
  the new status-update endpoints have someone authorized to call them.

**New features (small, additive, backend-only in this pass)**
- Budget summary now includes a per-category spend breakdown and total BNPL exposure (built from
  data that was already being recorded per-transaction but never aggregated).
- Grievance responses now include `daysSinceFiled` and an `escalationDue` flag once a bank-routed
  complaint passes the 30-day RBI Ombudsman threshold.
- Scam patterns can now be community-confirmed via `POST /fraud/patterns/{id}/confirm` (demo-scope:
  not yet deduped per user — see the code comment in `FraudService`).

**Deliberately out of scope for this pass** — these are new subsystems, not gaps to patch, and
need their own design pass: the notification service / event bus, an admin UI (the admin endpoints
above are API-only for now), offline-first support in the Angular UI, and the GenAI/agentic
features discussed separately. The Angular frontend also hasn't been wired up to any of the new
endpoints yet — this revision is backend-only.

⚠️ This code hasn't been build-verified in this environment (no Maven Central access here to run
`mvn compile`) — please run `mvn clean compile` locally before relying on it, and open an issue if
anything doesn't compile.

## What's simplified vs. the full blueprint, and why

- **Auth**: hand-rolled JWT issuance instead of OAuth2/OIDC via Cognito/Keycloak — same shape
  (short-lived bearer tokens, stateless), swappable later without touching business logic.
- **Data store**: H2 in-memory instead of PostgreSQL + MongoDB — same JPA code works against
  Postgres by changing the datasource URL/driver in `application.yml`.
- **No event bus**: fraud reports and grievance filings are saved directly; the code comments
  mark exactly where a `FraudAlertRaised` / `ComplaintFiled` event would be published to SNS/SQS
  for `notification-service` and `audit-compliance-service` to react to.
- **Not built**: `transaction-monitor-service` (needs a real Account Aggregator data-sharing
  partnership — flagged as an open decision in the blueprint), `mule-check-service` (needs
  RBI/NPCI data cooperation), the AWS/Azure infra, Terraform, and the API gateway layer
  (Apigee / AWS API Gateway) — CORS + JWT validation are done in-app instead for the demo.
