import {motion} from 'framer-motion';

export default function KpiCard({label, value, sub, accent = '#34e0a1', icon: Icon, delay = 0}) {
    return (
        <motion.div
            initial={{opacity: 0, y: 10}}
            animate={{opacity: 1, y: 0}}
            transition={{delay, duration: 0.4, ease: [0.22, 1, 0.36, 1]}}
            className="panel panel-pad relative overflow-hidden"
        >
            <div className="absolute right-0 top-0 h-full w-1" style={{background: accent, opacity: 0.5}}/>
            <div className="flex items-start justify-between">
                <div className="label-kicker">{label}</div>
                {Icon && <Icon size={16} style={{color: accent}} className="opacity-80"/>}
            </div>
            <div className="stat-num mt-3 text-3xl font-bold tracking-tight text-ink">{value}</div>
            {sub && <div className="mono mt-1.5 text-[11px] text-faint">{sub}</div>}
        </motion.div>
    );
}
