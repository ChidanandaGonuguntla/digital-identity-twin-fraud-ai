# Digital Identity Twin Enterprise React UI

Production-grade React + TypeScript UI for a Digital Identity Twin fraud command center.

## What is included

- Enterprise login page
- Welcome / landing page
- Fraud command center dashboard
- Live decisions with WebSocket + simulator fallback
- Rich Recharts visualizations
- Model monitoring page
- Twin explorer page
- Audit trail page
- Step-up challenge page
- Protected routes and auth store
- REST client and WebSocket reconnect hook
- Dark enterprise visual system

## Run

```bash
npm install
cp .env.example .env
npm run dev
```

Open:

```text
http://localhost:5173
```

## Backend endpoints expected

```text
POST /api/v1/fraud/decisions
GET  /api/v1/models/metadata
GET  /api/v1/audit/decisions
GET  /api/v1/twins/{customerId}
WS   /ws/decisions
```

Set `VITE_USE_MOCK=true` to run the UI without backend.


## Dependency note

This UI uses `recharts` `^3.0.0` to avoid the deprecated Recharts 1.x/2.x branches. The chart components use standard Recharts primitives that are compatible with v3.
