const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || 'API request failed');
  }
  return response.json();
}

export const api = {
  dashboard: () => request('/dashboard'),
  twins: () => request('/twins'),
  twin: (customerId) => request(`/twins/${customerId}`),
  recentEvents: () => request('/events/recent'),
  evaluate: (payload) => request('/fraud/evaluate', { method: 'POST', body: JSON.stringify(payload) }),
  feedback: (payload) => request('/fraud/feedback', { method: 'POST', body: JSON.stringify(payload) }),
  datasetSample: () => request('/dataset/sample'),
};

export const streamUrl = `${API_BASE}/events/stream`;
