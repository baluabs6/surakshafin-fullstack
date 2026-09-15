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
| GET | `/api/v1/users/me` | JWT | Current profile |
| GET | `/api/v1/fraud/patterns` | JWT | Scam pattern library |
| POST | `/api/v1/fraud/reports` | JWT | One-tap fraud report |
| GET | `/api/v1/fraud/reports/mine` | JWT | My submitted reports |
| POST | `/api/v1/grievances` | JWT | Run the routing wizard, file + auto-draft a complaint |
| GET | `/api/v1/grievances/mine` | JWT | My grievances |
| GET | `/api/v1/grievances/{id}` | JWT | One grievance, incl. generated complaint text |
| POST | `/api/v1/budget/transactions` | JWT | Log a spend transaction |
| GET | `/api/v1/budget/transactions` | JWT | List transactions |
| PUT | `/api/v1/budget/limit` | JWT | Set monthly budget |
| GET | `/api/v1/budget/summary` | JWT | Spend vs. budget + plain-language nudge |
| GET | `/api/v1/literacy` | none | Vernacular literacy content (`?language=hi&topic=UPI_SAFETY`) |

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
