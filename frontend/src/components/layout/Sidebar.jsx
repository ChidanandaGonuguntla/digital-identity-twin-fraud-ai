import {NavLink} from 'react-router-dom';
import {Fingerprint, LayoutDashboard, Radio, ScanSearch, ShieldCheck} from 'lucide-react';

const NAV = [
    {to: '/', label: 'Operations', icon: LayoutDashboard, end: true},
    {to: '/feed', label: 'Live Feed', icon: Radio},
    {to: '/assess', label: 'Assess', icon: ScanSearch},
    {to: '/twins', label: 'Identity Twins', icon: Fingerprint},
];

export default function Sidebar() {
    return (
        <aside className="flex w-[236px] shrink-0 flex-col border-r border-line bg-surface/60 backdrop-blur-md">
            <div className="flex items-center gap-2.5 px-5 py-5">
                <div className="grid h-9 w-9 place-items-center rounded-lg bg-signal/15 ring-1 ring-signal/30">
                    <ShieldCheck size={18} className="text-signal"/>
                </div>
                <div className="leading-tight">
                    <div className="font-extrabold tracking-tight text-ink">SENTINEL</div>
                    <div className="label-kicker">Twin Fraud Intel</div>
                </div>
            </div>

            <nav className="flex flex-col gap-1 px-3 py-2">
                {NAV.map(({to, label, icon: Icon, end}) => (
                    <NavLink key={to} to={to} end={end}
                             className={({isActive}) => `nav-item relative ${isActive ? 'nav-item-active' : ''}`}>
                        {({isActive}) => (
                            <>
                                {isActive && <span className="nav-rail top-1.5 bottom-1.5"/>}
                                <Icon size={17} className={isActive ? 'text-signal' : ''}/>
                                {label}
                            </>
                        )}
                    </NavLink>
                ))}
            </nav>

            <div className="mt-auto p-4">
                <div className="rounded-lg border border-line bg-base/40 p-3.5">
                    <div className="label-kicker mb-2">Detection stack</div>
                    <ul className="space-y-1.5 text-xs text-muted">
                        <li className="flex items-center gap-2"><Dot/> Deterministic rules</li>
                        <li className="flex items-center gap-2"><Dot/> LightGBM · ONNX</li>
                        <li className="flex items-center gap-2"><Dot/> Behavioral twin</li>
                    </ul>
                </div>
                <p className="mono mt-3 px-1 text-[10px] leading-relaxed text-faint">
                    v1.0 · Kafka · Valkey · Spring
                </p>
            </div>
        </aside>
    );
}

const Dot = () => <span className="h-1 w-1 rounded-full bg-signal/70"/>;
