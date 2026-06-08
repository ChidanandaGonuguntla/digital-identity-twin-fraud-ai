import {config} from './config';
import {makeTwinSnapshot, simulateAssessment} from './simulator';

// ---- REST client for the Spring Boot fraud backend ----
//
// Endpoints (from the Digital Twin Identity service):
//   POST /api/v1/transactions/assess   -> RiskAssessment
//   POST /api/v1/transactions/seed     -> 200
//   GET  /api/v1/twins/{customerId}    -> TwinSnapshot
//
// In mock mode these resolve against the local simulator so the UI is fully
// functional without a running backend.

async function http(path, options = {}) {
    const res = await fetch(`${config.apiBaseUrl}${path}`, {
        headers: {'Content-Type': 'application/json'},
        ...options,
    });
    if (!res.ok) {
        const body = await res.text().catch(() => '');
        throw new Error(`${res.status} ${res.statusText}${body ? ` — ${body}` : ''}`);
    }
    return res.status === 204 ? null : res.json();
}

export async function assessTransaction(event) {
    if (config.useMock) {
        await delay(280);
        return simulateAssessment(event);
    }
    return http('/transactions/assess', {method: 'POST', body: JSON.stringify(event)});
}

export async function seedTransactions(events) {
    if (config.useMock) {
        await delay(200);
        return `Seeded ${events.length} transactions (mock)`;
    }
    return http('/transactions/seed', {method: 'POST', body: JSON.stringify(events)});
}

export async function getTwin(customerId) {
    if (config.useMock) {
        await delay(260);
        return makeTwinSnapshot(customerId);
    }
    return http(`/twins/${encodeURIComponent(customerId)}`, {method: 'GET'});
}

const delay = (ms) => new Promise((r) => setTimeout(r, ms));
