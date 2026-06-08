import {useEffect, useState} from 'react';
import {Activity, Bell, Pause, Play} from 'lucide-react';
import {useStore} from '../../store/useStore';
import {config} from '../../services/config';

const STATUS_META = {
    live: {label: 'Live', color: '#34e0a1'},
    connecting: {label: 'Connecting', color: '#ffb24d'},
    reconnecting: {label: 'Reconnecting', color: '#ffb24d'},
    offline: {label: 'Offline', color: '#ff5168'},
};

export default function Topbar({onBell}) {
    const status = useStore((s) => s.status);
    const unread = useStore((s) => s.unread);
    const paused = useStore((s) => s.paused);
    const togglePause = useStore((s) => s.togglePause);
    const meta = STATUS_META[status] || STATUS_META.offline;
    const clock = useClock();

    return (
        <header
            className="flex h-16 shrink-0 items-center justify-between border-b border-line bg-surface/50 px-6 backdrop-blur-md">
            <div className="flex items-center gap-4">
                <div className="flex items-center gap-2.5 rounded-lg border border-line bg-base/40 px-3 py-1.5">
                    {status === 'live' ? <span className="live-dot"/> :
                        <span className="h-2 w-2 rounded-full" style={{background: meta.color}}/>}
                    <span className="mono text-xs font-medium" style={{color: meta.color}}>{meta.label}</span>
                    <span className="mono text-[10px] text-faint">{config.useMock ? 'SIMULATED' : 'BACKEND'}</span>
                </div>
                <div className="hidden items-center gap-2 text-faint md:flex">
                    <Activity size={14}/>
                    <span className="mono text-xs">decisions stream</span>
                </div>
            </div>

            <div className="flex items-center gap-3">
                <span className="mono hidden text-sm tabular-nums text-muted sm:block">{clock} UTC</span>
                <button onClick={togglePause} className="btn-ghost px-3 py-2"
                        title={paused ? 'Resume stream' : 'Pause stream'}>
                    {paused ? <Play size={15}/> : <Pause size={15}/>}
                    <span className="hidden sm:inline">{paused ? 'Resume' : 'Pause'}</span>
                </button>
                <button onClick={onBell} className="relative btn-ghost px-3 py-2" title="Notifications">
                    <Bell size={16}/>
                    {unread > 0 && (
                        <span
                            className="absolute -right-1 -top-1 grid h-4 min-w-4 place-items-center rounded-full bg-block px-1 text-[10px] font-bold text-white">
              {unread > 99 ? '99+' : unread}
            </span>
                    )}
                </button>
            </div>
        </header>
    );
}

function useClock() {
    const [t, setT] = useState(now());
    useEffect(() => {
        const id = setInterval(() => setT(now()), 1000);
        return () => clearInterval(id);
    }, []);
    return t;
}

function now() {
    return new Date().toLocaleTimeString('en-GB', {timeZone: 'UTC', hour12: false});
}
