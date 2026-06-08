import {PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, ResponsiveContainer} from 'recharts';

export default function TwinRadar({dimensions}) {
    const data = Object.entries(dimensions || {}).map(([k, v]) => ({dim: k, value: v}));
    return (
        <ResponsiveContainer width="100%" height={300}>
            <RadarChart data={data} outerRadius="72%">
                <PolarGrid stroke="#1e2a35"/>
                <PolarAngleAxis dataKey="dim" tick={{fill: '#8595a3', fontSize: 10, fontFamily: 'JetBrains Mono'}}/>
                <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false}/>
                <Radar dataKey="value" stroke="#34e0a1" fill="#34e0a1" fillOpacity={0.22} strokeWidth={1.8}
                       isAnimationActive/>
            </RadarChart>
        </ResponsiveContainer>
    );
}
