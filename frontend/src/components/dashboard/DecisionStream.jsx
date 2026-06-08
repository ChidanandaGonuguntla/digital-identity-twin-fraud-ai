import {Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import {fmtTime} from '../../lib/format';

export default function DecisionStream({data}) {
    return (
        <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={data} margin={{top: 8, right: 8, left: -22, bottom: 0}}>
                <defs>
                    {[['allow', '#34e0a1'], ['challenge', '#ffb24d'], ['block', '#ff5168']].map(([id, c]) => (
                        <linearGradient key={id} id={`g-${id}`} x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor={c} stopOpacity={0.5}/>
                            <stop offset="100%" stopColor={c} stopOpacity={0.02}/>
                        </linearGradient>
                    ))}
                </defs>
                <XAxis dataKey="t" tickFormatter={fmtTime} stroke="#566976"
                       tick={{fontSize: 10, fontFamily: 'JetBrains Mono'}}
                       tickLine={false} axisLine={{stroke: '#1e2a35'}} minTickGap={40}/>
                <YAxis stroke="#566976" tick={{fontSize: 10, fontFamily: 'JetBrains Mono'}} tickLine={false}
                       axisLine={false} allowDecimals={false}/>
                <Tooltip labelFormatter={(t) => fmtTime(t)} cursor={{stroke: '#2b3a48'}}/>
                <Area type="monotone" dataKey="ALLOW" stackId="1" stroke="#34e0a1" fill="url(#g-allow)"
                      strokeWidth={1.5} isAnimationActive={false}/>
                <Area type="monotone" dataKey="CHALLENGE" stackId="1" stroke="#ffb24d" fill="url(#g-challenge)"
                      strokeWidth={1.5} isAnimationActive={false}/>
                <Area type="monotone" dataKey="BLOCK" stackId="1" stroke="#ff5168" fill="url(#g-block)"
                      strokeWidth={1.5} isAnimationActive={false}/>
            </AreaChart>
        </ResponsiveContainer>
    );
}
