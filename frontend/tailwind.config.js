/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          950: '#030712',
          900: '#0b0f19',
          800: '#151c2c',
          700: '#222f47',
          600: '#384f73'
        },
        neon: {
          purple: '#9333ea',
          cyan: '#06b6d4',
          emerald: '#10b981',
          amber: '#f59e0b',
          rose: '#ef4444'
        }
      },
      animation: {
        'shake': 'shake 0.5s ease-in-out',
        'flip': 'flip 0.5s ease-out forwards',
        'pop': 'pop 0.15s ease-out',
        'radar': 'radar 3s linear infinite',
      },
      keyframes: {
        shake: {
          '0%, 100%': { transform: 'translateX(0)' },
          '20%, 60%': { transform: 'translateX(-6px)' },
          '40%, 80%': { transform: 'translateX(6px)' },
        },
        flip: {
          '0%': { transform: 'rotateX(0deg)', background: 'transparent' },
          '45%': { transform: 'rotateX(90deg)', background: 'transparent' },
          '55%': { transform: 'rotateX(90deg)' },
          '100%': { transform: 'rotateX(0deg)' },
        },
        pop: {
          '0%': { transform: 'scale(0.8)' },
          '100%': { transform: 'scale(1.0)' },
        },
        radar: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        }
      }
    },
  },
  plugins: [],
}
