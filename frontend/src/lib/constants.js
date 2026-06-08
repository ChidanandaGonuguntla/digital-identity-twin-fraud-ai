// Decision metadata: drives colors, labels, and accents across the whole UI.
export const DECISIONS = {
    ALLOW: {label: 'Allow', color: '#34e0a1', tint: 'rgba(52,224,161,0.12)', text: 'text-allow'},
    CHALLENGE: {label: 'Challenge', color: '#ffb24d', tint: 'rgba(255,178,77,0.12)', text: 'text-challenge'},
    BLOCK: {label: 'Block', color: '#ff5168', tint: 'rgba(255,81,104,0.12)', text: 'text-block'},
};

// Human-readable fraud signal catalog (mirrors the DeviationScoringEngine / model SHAP features).
export const SIGNALS = {
    geo_velocity: 'Geo-velocity',
    amount_anomaly: 'Amount anomaly',
    new_device: 'New device',
    unusual_hour: 'Unusual hour',
    new_category: 'New category',
    velocity: 'Txn velocity',
};

export const MERCHANT_CATEGORIES = [
    'grocery', 'gas', 'restaurant', 'retail', 'pharmacy',
    'entertainment', 'travel', 'electronics', 'jewelry', 'gambling',
];

export function decisionFromScore(score) {
    if (score >= 70) return 'BLOCK';
    if (score >= 40) return 'CHALLENGE';
    return 'ALLOW';
}
