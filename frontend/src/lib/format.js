export const fmtCurrency = (n) =>
    new Intl.NumberFormat('en-US', {style: 'currency', currency: 'USD', maximumFractionDigits: 0}).format(n || 0);

export const fmtCurrency2 = (n) =>
    new Intl.NumberFormat('en-US', {style: 'currency', currency: 'USD'}).format(n || 0);

export const fmtNumber = (n) => new Intl.NumberFormat('en-US').format(n || 0);

export const fmtPct = (n, d = 1) => `${(n || 0).toFixed(d)}%`;

export const fmtTime = (ts) =>
    new Date(ts).toLocaleTimeString('en-US', {hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit'});

export const fmtDateTime = (ts) =>
    new Date(ts).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });

export function timeAgo(ts) {
    const s = Math.floor((Date.now() - ts) / 1000);
    if (s < 5) return 'now';
    if (s < 60) return `${s}s ago`;
    const m = Math.floor(s / 60);
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
}

// Coarse city label from lat/long, for the feed (purely cosmetic).
const CITY_ANCHORS = [
    [35.2271, -80.8431, 'Charlotte, US'], [40.7128, -74.006, 'New York, US'],
    [41.8781, -87.6298, 'Chicago, US'], [29.7604, -95.3698, 'Houston, US'],
    [33.749, -84.388, 'Atlanta, US'], [37.7749, -122.4194, 'San Francisco, US'],
    [47.6062, -122.3321, 'Seattle, US'], [25.7617, -80.1918, 'Miami, US'],
    [1.3521, 103.8198, 'Singapore'], [51.5074, -0.1278, 'London, UK'],
    [55.7558, 37.6173, 'Moscow, RU'], [-23.5505, -46.6333, 'São Paulo, BR'],
    [19.076, 72.8777, 'Mumbai, IN'],
];

export function cityFromCoords(lat, lon) {
    let best = 'Unknown';
    let bestD = Infinity;
    for (const [la, lo, name] of CITY_ANCHORS) {
        const d = (la - lat) ** 2 + (lo - lon) ** 2;
        if (d < bestD) {
            bestD = d;
            best = name;
        }
    }
    return best;
}
