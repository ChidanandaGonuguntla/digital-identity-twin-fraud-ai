import { create } from 'zustand';
import { env } from '@/config/env';
import type { DecisionEvent } from '@/types/fraud';
import { initialDecisions } from '@/lib/mockData';

const MAX_DECISIONS = 250;
const FLUSH_MS = 500;

type DecisionState = {
  decisions: DecisionEvent[];
  connectionStatus: 'connecting' | 'open' | 'closed' | 'mock';
  addDecision: (event: DecisionEvent) => void;
  setConnectionStatus: (status: DecisionState['connectionStatus']) => void;
};

let pending: DecisionEvent[] = [];
let flushTimer: ReturnType<typeof setTimeout> | null = null;

function scheduleFlush(set: (partial: Partial<DecisionState> | ((state: DecisionState) => Partial<DecisionState>)) => void) {
  if (flushTimer !== null) {
    return;
  }
  flushTimer = setTimeout(() => {
    flushTimer = null;
    if (pending.length === 0) {
      return;
    }
    const batch = pending;
    pending = [];
    set((state) => ({
      decisions: [...batch, ...state.decisions].slice(0, MAX_DECISIONS),
    }));
  }, FLUSH_MS);
}

export const useDecisionStore = create<DecisionState>((set) => ({
  decisions: env.useMock ? initialDecisions : [],
  connectionStatus: 'closed',
  addDecision: (event) => {
    pending.unshift(event);
    if (pending.length > MAX_DECISIONS) {
      pending = pending.slice(0, MAX_DECISIONS);
    }
    scheduleFlush(set);
  },
  setConnectionStatus: (connectionStatus) => set({ connectionStatus }),
}));
