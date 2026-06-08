import {config} from './config';
import {makeEvent} from './simulator';

export function createLiveStream({onEvent, onStatus}) {
    let socket = null;
    let timer = null;
    let retry = 0;
    let stopped = false;
    let connecting = false;
    let connectId = 0;

    function setStatus(s) {
        onStatus?.(s);
    }

    function cleanupSocket() {
        if (!socket) return;

        socket.onopen = null;
        socket.onmessage = null;
        socket.onerror = null;
        socket.onclose = null;

        if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
            socket.close();
        }

        socket = null;
    }

    function connectReal() {
        if (stopped || connecting) return;

        const activeId = ++connectId;
        connecting = true;
        setStatus('connecting');

        console.log('Connecting WS:', config.wsUrl);

        let nextSocket;

        try {
            nextSocket = new WebSocket(config.wsUrl);
        } catch (e) {
            console.error('WebSocket creation failed:', e);
            connecting = false;
            scheduleReconnect();
            return;
        }

        socket = nextSocket;

        nextSocket.onopen = () => {
            if (stopped || activeId !== connectId) return;

            connecting = false;
            retry = 0;
            setStatus('live');
            console.log('WebSocket connected');
        };

        nextSocket.onmessage = (msg) => {
            if (stopped || activeId !== connectId) return;

            try {
                onEvent(JSON.parse(msg.data));
            } catch (e) {
                console.warn('Malformed WebSocket message:', msg.data);
            }
        };

        nextSocket.onerror = (event) => {
            if (stopped || activeId !== connectId) return;
            console.error('WebSocket error:', event);
        };

        nextSocket.onclose = (event) => {
            if (activeId !== connectId) return;

            connecting = false;
            socket = null;

            if (stopped) return;

            console.warn('WebSocket closed:', {
                code: event.code, reason: event.reason, wasClean: event.wasClean,
            });

            scheduleReconnect();
        };
    }

    function scheduleReconnect() {
        if (stopped) return;

        setStatus('reconnecting');

        const wait = Math.min(15000, 1000 * 2 ** retry++);

        clearTimeout(timer);

        timer = setTimeout(() => {
            if (!stopped) connectReal();
        }, wait);
    }

    function connectMock() {
        setStatus('connecting');

        setTimeout(() => {
            if (stopped) return;

            setStatus('live');

            const tick = () => {
                if (stopped) return;

                onEvent(makeEvent());
                timer = setTimeout(tick, 600 + Math.random() * 1600);
            };

            tick();
        }, 500);
    }

    function start() {
        if (socket || connecting) return;

        stopped = false;

        console.log('Live stream config:', {
            useMock: config.useMock, wsUrl: config.wsUrl,
        });

        if (config.useMock) {
            connectMock();
        } else {
            connectReal();
        }
    }

    function stop() {
        stopped = true;
        connecting = false;
        connectId += 1;

        clearTimeout(timer);
        cleanupSocket();

        setStatus('offline');
    }

    return {start, stop};
}