import {create} from 'zustand';
import {config} from '../services/config';
import {createLiveStream} from '../services/websocket';

let stream = null;
let idCounter = 0;

export const useStore = create((set, get) => ({
    status: 'offline',          // offline | connecting | live | reconnecting
    transactions: [],           // newest first
    notifications: [],          // BLOCK / high CHALLENGE alerts
    unread: 0,
    paused: false,

    setStatus: (status) => set({status}),
    togglePause: () => set((s) => ({paused: !s.paused})),

    ingest: (event) => {
        if (get().paused) return;
        set((s) => {
            const transactions = [event, ...s.transactions].slice(0, config.feedLimit);
            let {notifications, unread} = s;
            if (event.decision === 'BLOCK' || (event.decision === 'CHALLENGE' && event.riskScore >= 55)) {
                const note = {...event, _nid: ++idCounter};
                notifications = [note, ...notifications].slice(0, 60);
                unread = unread + 1;
            }
            return {transactions, notifications, unread};
        });
    },

    markRead: () => set({unread: 0}),
    clearNotifications: () => set({notifications: [], unread: 0}),

    start: () => {
        if (stream) return;
        stream = createLiveStream({
            onEvent: (e) => get().ingest(e),
            onStatus: (status) => get().setStatus(status),
        });
        stream.start();
    },
    stop: () => {
        stream?.stop();
        stream = null;
    },
}));

// ---- Derived selectors (computed from the live buffer) ----

export function selectKpis(txns) {
    const total = txns.length;
    const blocked = txns.filter((t) => t.decision === 'BLOCK');
    const challenged = txns.filter((t) => t.decision === 'CHALLENGE').length;
    const allowed = total - blocked.length - challenged;
    const prevented = blocked.reduce((a, t) => a + (t.amount || 0), 0);
    const avgScore = total ? txns.reduce((a, t) => a + t.riskScore, 0) / total : 0;
    return {
        total,
        blocked: blocked.length,
        challenged,
        allowed,
        approvalRate: total ? (allowed / total) * 100 : 0,
        blockRate: total ? (blocked.length / total) * 100 : 0,
        avgScore,
        prevented,
    };
}

export function selectScoreHistogram(txns) {
    const buckets = Array.from({length: 10}, (_, i) => ({range: `${i * 10}-${i * 10 + 9}`, count: 0, bucket: i}));
    txns.forEach((t) => {
        const i = Math.min(9, Math.floor(t.riskScore / 10));
        buckets[i].count += 1;
    });
    return buckets;
}

export function selectSignalLeaderboard(txns) {
    const agg = {};
    txns.forEach((t) => (t.signals || []).forEach((s) => {
        agg[s.name] = agg[s.name] || {name: s.name, total: 0, count: 0};
        agg[s.name].total += s.contribution;
        agg[s.name].count += 1;
    }));
    return Object.values(agg)
        .map((s) => ({name: s.name, avg: s.total / s.count, count: s.count}))
        .sort((a, b) => b.count * b.avg - a.count * a.avg)
        .slice(0, 6);
}

// Decisions-over-time series, bucketed into the last N 5-second windows.
export function selectDecisionStream(txns, windows = 24, windowMs = 5000) {
    const now = Date.now();
    const series = Array.from({length: windows}, (_, i) => {
        const end = now - (windows - 1 - i) * windowMs;
        return {t: end, ALLOW: 0, CHALLENGE: 0, BLOCK: 0};
    });
    txns.forEach((tx) => {
        const idx = windows - 1 - Math.floor((now - tx.timestamp) / windowMs);
        if (idx >= 0 && idx < windows) series[idx][tx.decision] += 1;
    });
    return series;
}
