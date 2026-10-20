/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        zeekr: {
          blue: '#3B5BDB',
          'blue-hover': '#4C6EF5',
          orange: '#F76707',
          'orange-tag': '#D94826',
          red: '#E03131',
          green: '#2B8A3E',
          dark: '#141517',
          card: 'var(--surface-glass-primary)',
        },
        receiver: {
          50: '#ecfdf5',
          100: '#d1fae5',
          400: '#34d399',
          500: '#10b981',
          600: '#059669',
        },
        client: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          400: '#38bdf8',
          500: '#0284c7',
          600: '#0369a1',
        },
      },
      borderRadius: {
        'zeekr-modal': '24px',
        'zeekr-card': '20px',
        'zeekr-btn': '14px',
        'zeekr-app': '14px',
      },
      fontFamily: {
        sans: [
          '"HarmonyOS Sans SC"',
          '"MiSans"',
          '"PingFang SC"',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Inter"',
          '"DIN Pro"',
          'sans-serif',
        ],
      },
      boxShadow: {
        'zeekr-soft': 'var(--card-shadow-soft)',
        'zeekr-elevated': 'var(--card-shadow-elevated)',
        'glow-receiver': '0 0 35px -5px rgba(43, 138, 62, 0.3)',
        'glow-client': '0 0 35px -5px rgba(59, 91, 219, 0.3)',
      }
    },
  },
  plugins: [],
}

