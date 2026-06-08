/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{js,jsx}'],
    theme: {
        extend: {
            colors: {
                base: '#070a0d',
                surface: '#0d1219',
                surface2: '#131c25',
                raised: '#18222d',
                line: '#1e2a35',
                line2: '#2b3a48',
                ink: '#e9eff4',
                muted: '#8595a3',
                faint: '#566976',
                signal: '#34e0a1',
                allow: '#34e0a1',
                challenge: '#ffb24d',
                block: '#ff5168',
                azure: '#5aa2ff',
                violet: '#9b8cff',
            },
            fontFamily: {
                sans: ['Archivo', 'ui-sans-serif', 'system-ui', 'sans-serif'],
                mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
            },
            boxShadow: {
                glow: '0 0 0 1px rgba(52,224,161,0.25), 0 0 24px -4px rgba(52,224,161,0.35)',
                'glow-block': '0 0 0 1px rgba(255,81,104,0.35), 0 0 30px -4px rgba(255,81,104,0.45)',
                panel: '0 1px 0 0 rgba(255,255,255,0.03) inset, 0 18px 40px -24px rgba(0,0,0,0.9)',
            },
            keyframes: {
                fadeUp: {
                    '0%': {opacity: 0, transform: 'translateY(8px)'},
                    '100%': {opacity: 1, transform: 'translateY(0)'}
                },
                blink: {'0%,100%': {opacity: 1}, '50%': {opacity: 0.25}},
            },
            animation: {
                fadeUp: 'fadeUp 0.5s cubic-bezier(0.22,1,0.36,1) both',
                blink: 'blink 1.4s ease-in-out infinite',
            },
        },
    },
    plugins: [],
}
