import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e6f7f7',
          100: '#b3e6e6',
          200: '#80d4d4',
          300: '#4dc3c3',
          400: '#1ab1b1',
          500: '#00a0a0',
          600: '#008080',
          700: '#006060',
          800: '#004040',
          900: '#002020',
        },
        secondary: {
          50: '#f5f3f0',
          100: '#e6dfd5',
          200: '#d7cbb9',
          300: '#c8b79e',
          400: '#b9a383',
          500: '#aa8f67',
          600: '#886f52',
          700: '#66533e',
          800: '#443729',
          900: '#221c15',
        },
      },
    },
  },
  plugins: [],
};

export default config;