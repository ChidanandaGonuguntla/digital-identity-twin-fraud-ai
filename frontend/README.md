# SENTINEL — Digital Twin Fraud Intelligence Console

A production-grade React frontend for the Digital Twin Identity fraud system: a
real-time fraud-ops command console with live transaction decisioning, WebSocket
alerts, manual assessment, and behavioral-twin inspection.

It runs **live out of the box** on a built-in event simulator, and switches to your
Spring Boot backend by flipping one environment variable.

## Features

- **Operations dashboard** — live KPIs (throughput, approval/block rates, avg risk,
  exposure prevented), a stacked decision-stream chart, risk-score histogram, and a
  fraud-signal leaderboard, all updating in real time.
- **Live Feed** — every scored transaction streams in with a color flash by decision;
  filter by ALLOW / CHALLENGE / BLOCK; click any row for a full assessment drawer with
  a risk gauge, signal contributions, and reason codes.
- **Assess** — submit a transaction to the scoring engine (`POST /transactions/assess`)
  and see the gauge, decision, signal breakdown, and reason codes. Includes Normal/Fraud
  presets.
- **Identity Twins** — look up a customer (`GET /twins/{id}`) and inspect their
  behavioral baseline with a stability radar (amount, location, device, temporal,
  category, velocity dimensions).
- **WebSocket notifications** — high-risk decisions raise live toasts and collect in a
  slide-over alert center with an unread badge.

## Stack

React 18 · Vite 5 · React Router 6 · Zustand · Recharts · Framer Motion · Tailwind CSS ·
Lucide icons. Fonts: Archivo (display) + JetBrains Mono (data).

## Run

```bash
npm install
npm run dev      # http://localhost:5173  (simulated data by default)
npm run build    # production bundle in dist/
```

## Connect to your backend

Copy `.env.example` to `.env` and set:

```
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api/v1
VITE_WS_URL=ws://localhost:8080/ws/decisions
```

`vite.config.js` already proxies `/api` and `/ws` to `localhost:8080` in dev, so no CORS
setup is needed locally.

### REST contract (already wired in `src/services/api.js`)

| Call                       | Endpoint                           | Returns          |
|----------------------------|------------------------------------|------------------|
| `assessTransaction(event)` | `POST /api/v1/transactions/assess` | `RiskAssessment` |
| `seedTransactions(events)` | `POST /api/v1/transactions/seed`   | ok               |
| `getTwin(customerId)`      | `GET /api/v1/twins/{customerId}`   | `TwinSnapshot`   |

### WebSocket contract (`src/services/websocket.js`)

The console connects to `VITE_WS_URL` and expects each message to be a JSON object
representing a decided transaction:

```jsonc
{
  "transactionId": "TXN-...",
  "customerId": "CUST-01007",
  "amount": 1450.00,
  "merchantCategory": "electronics",
  "deviceId": "dev-unknown-x",
  "latitude": 1.3521,
  "longitude": 103.8198,
  "timestamp": 1733600000000,        // epoch ms
  "riskScore": 84.0,                  // 0–100
  "decision": "BLOCK",                // ALLOW | CHALLENGE | BLOCK
  "coldStart": false,
  "signals": [                        // optional; drives the breakdown UI
    { "name": "geo_velocity", "contribution": 38.0 },
    { "name": "new_device", "contribution": 18.0 }
  ],
  "reasons": ["Impossible travel detected ...", "Unrecognized device fingerprint"]
}
```

On the Spring side, publish your decisions topic to this socket. If you use
**STOMP/SockJS** (common with Spring WebSocket), swap `connectReal()` in
`websocket.js` for a `@stomp/stompjs` client subscribing to `/topic/decisions` — the
`onEvent` callback contract is identical. The `signals` array maps directly to your
model's SHAP feature contributions; `reasons` to the deterministic engine's reason codes.

## How it maps to the system we built

```
Kafka decisions topic ──► Spring WebSocket ──► VITE_WS_URL ──► Live Feed / Toasts / KPIs
POST /assess           ◄── Assess page (manual scoring)
GET /twins/{id}        ◄── Identity Twins page (behavioral baseline)
```

The `signals` and `reasons` fields are the explainability surface — the model's SHAP
contributions and the rule engine's reason codes rendered for the analyst.

## Project structure

```
src/
  services/   config · api (REST) · websocket (live stream) · simulator (mock data)
  store/      useStore (Zustand) + derived selectors (KPIs, charts)
  lib/        constants (decisions, signals) · formatters
  components/ layout · common · dashboard · feed · notifications · twin
  pages/      Operations · LiveFeed · Assess · Twins
```
