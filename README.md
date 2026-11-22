# LedgerFlow

[![CI](https://github.com/akkupratap323/ledgerflow/actions/workflows/ci.yml/badge.svg)](https://github.com/akkupratap323/ledgerflow/actions/workflows/ci.yml)

A double-entry ledger and wallet service written in Java 21 and Spring Boot 3.

Every fintech, marketplace and wallet app needs a ledger: a place where money moves are recorded so
that balances are always correct, nothing is posted twice and every rupee can be traced. LedgerFlow is
that core, the kind of service that sits behind UPI wallets, payouts, marketplaces and card authorizations.

## What it does

- **Double-entry accounting.** Every journal entry must balance (debits = credits). Accounts have types
  (asset, liability, equity, revenue, expense) and a normal balance side.
- **Money as integers.** Amounts are stored in minor units (paise, cents). No floating point anywhere.
- **Idempotent posting.** Send an `Idempotency-Key` header and retries return the original entry. Reusing a
  key with a different body is rejected with `409`. Safe under concurrent retries.
- **No overdrafts under load.** Accounts are row-locked in a fixed order while posting, so parallel spends
  can never overdraw a wallet and two entries can never deadlock.
- **Transfers with fees.** One call moves money from A to B and books the fee to a revenue account.
- **Holds (authorize, capture, void).** Reserve funds now, capture all or part later, or void. Held funds
  cannot be spent. Expired holds are released by a scheduled job.
- **Reversals.** Entries are immutable. Mistakes are fixed by posting a mirror entry, at most once.
- **Account lifecycle.** Freeze (blocks postings), unfreeze, close (only at zero balance).
- **Statements.** Paginated, date-filtered, with a gapless per-account sequence and the running balance after
  each posting.
- **Trial balance and reconciliation.** Check that the books balance per currency, and recompute every
  balance from raw postings to catch drift or manual database edits.
- **Transactional outbox + signed webhooks.** Ledger events are written in the same transaction as the money
  move, then delivered with HMAC-SHA256 signatures, exponential backoff and dead-lettering.
- **Production basics.** API keys, request ids in logs, RFC 9457 problem responses with stable error codes,
  Flyway migrations, health probes, Prometheus metrics, OpenAPI/Swagger UI, Docker and CI on PostgreSQL.

## Architecture

```
            HTTP (X-API-Key, Idempotency-Key, X-Request-Id)
                               |
   +-----------+-----------+---+--------+-----------+-----------+
   | accounts  |  entries  | transfers  |   holds   |  reports  |
   +-----------+-----+-----+-----+------+-----+-----+-----------+
                     |           |            |
                     v           v            v
               +--------------------------------------+
               | LedgerService                        |
               |  validate -> lock accounts (id order)|
               |  -> apply postings -> save entry     |
               |  -> write outbox event   (1 tx)      |
               +------------------+-------------------+
                                  |
     PostgreSQL: accounts, journal_entries, postings, holds, outbox_events
                                  |
                       OutboxRelay (scheduled)
                                  |
                   signed webhook POST, retry, DLQ
```

| Package | Responsibility |
| --- | --- |
| `account` | Accounts, types, status lifecycle, held and available balance |
| `journal` | Journal entries, postings, validation, idempotency, reversals |
| `transfer` | Simple A to B transfers with optional fee |
| `hold` | Authorize, capture, void and expiry of reserved funds |
| `report` | Statements, trial balance, reconciliation |
| `outbox` | Transactional outbox, relay, webhook signing |
| `web` | Error mapping, API key and request id filters |

## Run it

Requirements: Java 21. Docker is optional.

```bash
./gradlew bootRun          # in-memory H2, API open, Swagger at http://localhost:8080/swagger-ui.html
./gradlew test             # unit, integration and concurrency tests
docker compose up --build  # PostgreSQL + app, API key: dev-key-change-me
```

## Try it

```bash
B=http://localhost:8080/api/v1
H='Content-Type: application/json'

# Open accounts
curl -s -H "$H" -X POST $B/accounts -d '{"code":"cash:bank","name":"Bank","type":"ASSET","currency":"INR","allowNegative":true}'
curl -s -H "$H" -X POST $B/accounts -d '{"code":"wallet:asha","name":"Asha","type":"LIABILITY","currency":"INR"}'
curl -s -H "$H" -X POST $B/accounts -d '{"code":"wallet:ravi","name":"Ravi","type":"LIABILITY","currency":"INR"}'
curl -s -H "$H" -X POST $B/accounts -d '{"code":"revenue:fees","name":"Fees","type":"REVENUE","currency":"INR"}'

# Top up Asha's wallet with INR 1,000 (safe to retry)
curl -s -H "$H" -H "Idempotency-Key: upi-881" -X POST $B/entries -d '{
  "reference":"upi-881","currency":"INR","postings":[
    {"account":"cash:bank","side":"DEBIT","amountMinor":100000},
    {"account":"wallet:asha","side":"CREDIT","amountMinor":100000}]}'

# Asha sends Ravi INR 250 with a INR 5 fee
curl -s -H "$H" -X POST $B/transfers -d '{"from":"wallet:asha","to":"wallet:ravi","amountMinor":25000,
  "currency":"INR","feeMinor":500,"feeAccount":"revenue:fees"}'

# Reserve INR 300 for an order, then capture INR 200 of it
curl -s -H "$H" -X POST $B/holds -d '{"account":"wallet:asha","amountMinor":30000,"reference":"order-9","ttlSeconds":600}'
curl -s -H "$H" -X POST $B/holds/{holdId}/capture -d '{"amountMinor":20000,"destination":"wallet:ravi"}'

# Reports
curl -s $B/reports/trial-balance
curl -s $B/reports/reconciliation
curl -s "$B/accounts/{accountId}/statement?page=0&size=50"
```

## API

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/accounts` | Open an account |
| GET | `/api/v1/accounts`, `/{id}`, `/by-code/{code}` | Read accounts |
| POST | `/api/v1/accounts/{id}/freeze`, `/unfreeze`, `/close` | Lifecycle |
| GET | `/api/v1/accounts/{id}/statement` | Statement (`from`, `to`, `page`, `size`) |
| POST | `/api/v1/entries` | Post a journal entry (`Idempotency-Key` supported) |
| GET | `/api/v1/entries/{id}` | Read an entry |
| POST | `/api/v1/entries/{id}/reversal` | Reverse an entry |
| POST | `/api/v1/transfers` | Transfer with optional fee |
| POST | `/api/v1/holds`, `/{id}/capture`, `/{id}/void` | Holds |
| GET | `/api/v1/reports/trial-balance` | Trial balance per currency |
| GET | `/api/v1/reports/reconciliation` | Recompute balances from postings |

Errors use RFC 9457 problem details with a stable `code`, for example:

```json
{"status":422,"detail":"Account wallet:ravi has insufficient funds","code":"insufficient_funds"}
```

## Configuration

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=postgres` | Use PostgreSQL instead of in-memory H2 |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection |
| `LEDGERFLOW_API_KEYS` | Comma separated API keys. Empty means the API is open |
| `LEDGERFLOW_WEBHOOK_URL`, `LEDGERFLOW_WEBHOOK_SECRET` | Webhook target and signing secret |

Webhook receivers should verify `X-LedgerFlow-Signature` = `sha256=HMAC(secret, timestamp + "." + body)`
using `X-LedgerFlow-Timestamp`, and dedupe on `X-LedgerFlow-Event-Id` (delivery is at least once).

## Design notes

- **Why lock accounts in id order?** Two transfers A to B and B to A would deadlock if each locked its source
  first. Sorting ids gives every transaction the same lock order.
- **Why cache balances at all?** Summing postings on every read does not scale. The cached balance is updated
  in the same transaction as the postings, and the reconciliation report proves they agree.
- **Why an outbox instead of publishing directly?** Publishing inside the transaction can send events for
  writes that later roll back, and publishing after commit can lose events on a crash. The outbox has neither
  problem.

## License

MIT
