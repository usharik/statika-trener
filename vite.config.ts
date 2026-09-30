import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // relativní cesty: stejná sestava funguje na GitHub Pages (/statika-trener/) i na vlastním serveru
  base: './',
  // přístup přes Tailscale (MagicDNS jméno i IP)
  preview: { allowedHosts: ['.ts.net'] },
  server: { allowedHosts: ['.ts.net'] },
})
