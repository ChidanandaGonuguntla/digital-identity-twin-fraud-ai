import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// Dev proxy lets the frontend talk to the Spring backend without CORS config.
// Point VITE_USE_MOCK=false in .env to use these instead of the built-in simulator.
export default defineConfig({
    plugins: [react()], server: {
        port: 5173, proxy: {
            '/api': {target: 'http://localhost:9997', changeOrigin: true},
            '/ws': {target: 'ws://localhost:9997', ws: true},
        },
    },
})
