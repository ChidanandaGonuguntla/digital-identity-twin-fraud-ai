import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { env } from '@/config/env';
import { normalizeDecisionEvent } from '@/lib/decisionUtils';
import { createMockDecision } from '@/lib/mockData';
import { useDecisionStore } from '@/store/decisionStore';
import { getAccessToken } from '@/store/authStore';
function resolveWsUrl() {
  let url = env.wsUrl;
  if (import.meta.env.DEV || url.startsWith('/')) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const path = url.startsWith('/') ? url : '/ws/decisions';
    url = `${proto}//${window.location.host}${path}`;
  }
  if (!env.securityEnabled) return url;
  const token = getAccessToken();
  if (!token) return url;
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}token=${encodeURIComponent(token)}`;
}

export function useDecisionWebSocket() {
  const addDecision = useDecisionStore((s) => s.addDecision);
  const setConnectionStatus = useDecisionStore((s) => s.setConnectionStatus);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    let mockTimer: number | undefined;
    let active = true;

    const startMock = () => {
      setConnectionStatus('mock');
      mockTimer = window.setInterval(() => addDecision(createMockDecision()), 2500);
    };

    if (env.useMock) {
      startMock();
      return () => {
        active = false;
        if (mockTimer) window.clearInterval(mockTimer);
      };
    }

    setConnectionStatus('connecting');

    const client = new Client({
      brokerURL: resolveWsUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (!active) return;
        setConnectionStatus('open');
        client.subscribe('/topic/decisions', (message) => {
          try {
            const event = normalizeDecisionEvent(JSON.parse(message.body) as Record<string, unknown>);
            addDecision(event);
          } catch (error) {
            console.error('Invalid decision event payload', error);
          }
        });
      },
      onStompError: (frame) => {
        console.error('STOMP broker error', frame.headers.message);
        if (active) setConnectionStatus('closed');
      },
      onWebSocketClose: () => {
        if (active) setConnectionStatus('closed');
      },
      onDisconnect: () => {
        if (active) setConnectionStatus('closed');
      }
    });

    clientRef.current = client;
    client.activate();

    return () => {
      active = false;
      if (mockTimer) window.clearInterval(mockTimer);
      client.deactivate();
      clientRef.current = null;
    };
  }, [addDecision, setConnectionStatus]);
}
