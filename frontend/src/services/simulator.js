import {decisionFromScore} from '../lib/constants';

// ---------------------------------------------------------------------------
// Standalone event simulator.
// Generates realistic transaction + RiskAssessment objects so the console is
// fully alive without a backend. The emitted shape matches the WebSocket
// "decisions" message contract documented in the README.
// ---------------------------------------------------------------------------

const CUSTOMERS = Array.from({length: 40}, (_, i) => ({
    id: `CUST-${String(1001 + i).padStart(5, '0')}`,
    home: pick(HOME_CITIES()),
    typicalAmount: 30 + Math.random() * 180,
    categories: sample(CATEGORIES(), 2 + Math.floor(Math.random() * 3)),
    device: `dev-${1001 + i}-home`,
}));

function HOME_CITIES() {
    return [
        [35.2271, -80.8431], [40.7128, -74.006], [41.8781, -87.6298],
        [29.7604, -95.3698], [33.749, -84.388], [37.7749, -122.4194],
        [47.6062, -122.3321], [25.7617, -80.1918],
    ];
}

function FAR_CITIES() {
    return [[1.3521, 103.8198], [51.5074, -0.1278], [55.7558, 37.6173], [-23.5505, -46.6333], [19.076, 72.8777]];
}

function CATEGORIES() {
    return ['grocery', 'gas', 'restaurant', 'retail', 'pharmacy', 'entertainment', 'travel', 'electronics', 'jewelry', 'gambling'];
}

function pick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function sample(arr, n) {
    return [...arr].sort(() => Math.random() - 0.5).slice(0, n);
}

function jitter(v, j) {
    return v + (Math.random() - 0.5) * j;
}

let seq = 0;

// Generates one fully-decided live event.
export function makeEvent() {
    const cust = pick(CUSTOMERS);
    const roll = Math.random();
    // ~82% clean, ~11% suspicious (challenge), ~7% fraud (block)
    const profile = roll < 0.82 ? 'clean' : roll < 0.93 ? 'suspicious' : 'fraud';

    let amount = round2(lognormalish(cust.typicalAmount));
    let category = pick(cust.categories);
    let [lat, lon] = cust.home.map((c) => jitter(c, 0.25));
    let device = Math.random() < 0.9 ? cust.device : `dev-${cust.id}-alt`;
    const signals = {};

    if (profile !== 'clean') {
        const active = sample(['geo_velocity', 'amount_anomaly', 'new_device', 'unusual_hour', 'new_category'],
            profile === 'fraud' ? 2 + Math.floor(Math.random() * 3) : 1 + Math.floor(Math.random() * 2));
        if (active.includes('amount_anomaly')) {
            amount = round2(amount * (4 + Math.random() * 30));
            signals.amount_anomaly = 14 + Math.random() * 16;
        }
        if (active.includes('new_category')) {
            category = pick(CATEGORIES().filter((c) => !cust.categories.includes(c)));
            signals.new_category = 8 + Math.random() * 8;
        }
        if (active.includes('geo_velocity')) {
            const f = pick(FAR_CITIES());
            lat = jitter(f[0], 0.2);
            lon = jitter(f[1], 0.2);
            signals.geo_velocity = 22 + Math.random() * 18;
        }
        if (active.includes('new_device')) {
            device = `dev-fraud-${Math.floor(Math.random() * 1e6)}`;
            signals.new_device = 10 + Math.random() * 10;
        }
        if (active.includes('unusual_hour')) {
            signals.unusual_hour = 6 + Math.random() * 9;
        }
    }

    const score = Math.min(100, round2(Object.values(signals).reduce((a, b) => a + b, 0) + Math.random() * 4));
    const decision = decisionFromScore(score);

    return {
        transactionId: `TXN-${Date.now().toString(36).toUpperCase()}-${(seq++).toString().padStart(3, '0')}`,
        customerId: cust.id,
        amount,
        merchantCategory: category,
        deviceId: device,
        latitude: round4(lat),
        longitude: round4(lon),
        timestamp: Date.now(),
        riskScore: score,
        decision,
        coldStart: false,
        signals: Object.entries(signals)
            .map(([name, contribution]) => ({name, contribution: round2(contribution)}))
            .sort((a, b) => b.contribution - a.contribution),
        reasons: buildReasons(signals, amount, decision),
    };
}

function buildReasons(signals, amount, decision) {
    const r = [];
    if (signals.geo_velocity) r.push('Impossible travel detected between consecutive transactions');
    if (signals.amount_anomaly) r.push(`Amount $${amount.toFixed(2)} far exceeds the customer's typical spend`);
    if (signals.new_device) r.push('Unrecognized device fingerprint');
    if (signals.unusual_hour) r.push('Transaction at an hour the customer rarely transacts');
    if (signals.new_category) r.push('First-ever transaction in this merchant category');
    if (!r.length) r.push('Transaction consistent with the customer behavioral twin');
    if (decision === 'BLOCK') r.unshift('Composite deviation exceeds block threshold');
    return r;
}

// Assessment for a manually-submitted transaction (Assess page).
export function simulateAssessment(event) {
    const signals = {};
    const amt = Number(event.amount) || 0;
    if (amt > 1500) signals.amount_anomaly = Math.min(30, 10 + amt / 200);
    if (['jewelry', 'gambling', 'electronics'].includes(event.merchantCategory)) signals.new_category = 12;
    const farLat = Math.abs(event.latitude) < 24 || Math.abs(event.latitude) > 49;
    if (farLat) signals.geo_velocity = 30;
    if (String(event.deviceId || '').includes('unknown') || String(event.deviceId || '').includes('fraud')) signals.new_device = 18;
    const hour = new Date(event.timestamp || Date.now()).getUTCHours();
    if (hour < 5 || hour > 23) signals.unusual_hour = 12;

    const score = Math.min(100, round2(Object.values(signals).reduce((a, b) => a + b, 0)));
    const decision = decisionFromScore(score);
    return {
        transactionId: event.transactionId || `TXN-${Date.now().toString(36).toUpperCase()}`,
        customerId: event.customerId,
        riskScore: score,
        decision,
        coldStart: false,
        signals: Object.entries(signals).map(([name, contribution]) => ({name, contribution: round2(contribution)}))
            .sort((a, b) => b.contribution - a.contribution),
        reasons: buildReasons(signals, amt, decision),
        assessedAt: new Date().toISOString(),
    };
}

export function makeTwinSnapshot(customerId) {
    const seed = [...String(customerId)].reduce((a, c) => a + c.charCodeAt(0), 0);
    const rnd = (n) => ((seed * 9301 + n * 49297) % 233280) / 233280;
    const txnCount = 80 + Math.floor(rnd(1) * 900);
    const mean = round2(40 + rnd(2) * 160);
    return {
        customerId,
        transactionCount: txnCount,
        meanAmount: mean,
        stdDevAmount: round2(mean * (0.2 + rnd(3) * 0.4)),
        knownDevices: 1 + Math.floor(rnd(4) * 4),
        knownCategories: 3 + Math.floor(rnd(5) * 5),
        createdAt: new Date(Date.now() - (200 + rnd(6) * 500) * 86400000).toISOString(),
        lastUpdated: new Date(Date.now() - rnd(7) * 3600000).toISOString(),
        // Behavioral stability dimensions (0-100) for the radar chart.
        dimensions: {
            'Amount stability': 55 + Math.floor(rnd(8) * 40),
            'Location consistency': 50 + Math.floor(rnd(9) * 45),
            'Device trust': 60 + Math.floor(rnd(10) * 38),
            'Temporal regularity': 45 + Math.floor(rnd(11) * 50),
            'Category breadth': 40 + Math.floor(rnd(12) * 55),
            'Velocity normality': 58 + Math.floor(rnd(13) * 40),
        },
    };
}

function lognormalish(median) {
    const z = (Math.random() + Math.random() + Math.random() - 1.5) / 1.5;
    return Math.max(1, median * Math.exp(z * 0.5));
}

const round2 = (n) => Math.round(n * 100) / 100;
const round4 = (n) => Math.round(n * 10000) / 10000;
