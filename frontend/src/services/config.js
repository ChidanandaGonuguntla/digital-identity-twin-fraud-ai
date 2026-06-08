// Central runtime config. All values overridable via .env (see .env).
const env = import.meta.env;

export const config = {
    // REST base for the Spring backend (proxied to :8080 in dev via vite.config.js).
    apiBaseUrl: env.VITE_API_BASE_URL || '/api/v1',
    // WebSocket endpoint streaming the live decisions topic.
    wsUrl: env.VITE_WS_URL || 'ws://localhost:9997/ws/decisions',
    // When true, the UI runs on a built-in event simulator (no backend needed).
    useMock: (env.VITE_USE_MOCK ?? 'true') !== 'false',
    // Max transactions kept in the live buffer.
    feedLimit: 250,
};
