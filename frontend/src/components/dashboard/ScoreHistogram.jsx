import {Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

export default function ScoreHistogram({data}) {
    const colorFor = (bucket) => (bucket >= 7 ? '#ff5168' : bucket >= 4 ? '#ffb24d' : '#34e0a1');
    return (
        <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data} margin={{top: 8, right: 8, left: -22, bottom: 0}}>
                <XAxis dataKey="range" stroke="#566976" tick={{fontSize: 9, fontFamily: 'JetBrains Mono'}}
                       tickLine={false} axisLine={{stroke: '#1e2a35'}} interval={0}/>
                <YAxis stroke="#566976" tick={{fontSize: 10, fontFamily: 'JetBrains Mono'}} tickLine={false}
                       axisLine={false} allowDecimals={false}/>
                <Tooltip cursor={{fill: 'rgba(255,255,255,0.03)'}}/>
                <Bar dataKey="count" radius={[3, 3, 0, 0]} isAnimationActive={false}>
                    {data.map((d) => <Cell key={d.bucket} fill={colorFor(d.bucket)} fillOpacity={0.85}/>)}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}
