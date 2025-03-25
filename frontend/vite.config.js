import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';
import dotenv from 'dotenv';

// Load environment variables from .env files
dotenv.config();

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isProduction = mode === 'production';
  
  return {
    plugins: [
      // Add React plugin with JSX/TSX support
      react({
        // Enable Fast Refresh for development
        fastRefresh: !isProduction,
        // Include JSX runtime for React 18.2.0+
        jsxRuntime: 'automatic',
        // Enable TypeScript with Babel for JSX transformation
        babel: {
          presets: [
            ['@babel/preset-typescript', { isTSX: true, allExtensions: true }]
          ]
        }
      })
    ],
    
    // Configure server settings
    server: {
      port: 3000,
      open: true,
      // Configure API proxy for backend communication
      proxy: {
        // Proxy API requests to backend
        '/api': {
          target: 'http://localhost:5900',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      },
      // Enable CORS for development
      cors: true
    },
    
    // Configure build options
    build: {
      outDir: 'dist',
      // Optimize chunks for production
      rollupOptions: {
        output: {
          manualChunks: {
            // Split vendor code for better caching
            vendor: ['react', 'react-dom', 'react-router-dom'],
            // Payment-specific chunk
            payments: [
              '@reduxjs/toolkit',
              'react-redux',
              '@tanstack/react-query',
              'date-fns',
              'react-hook-form'
            ],
            // UI component libraries
            ui: ['@headlessui/react', '@tailwindcss/catalyst']
          }
        }
      },
      // Enable source maps for production debugging
      sourcemap: true,
      // Minify output for production
      minify: isProduction,
      // Target modern browsers
      target: 'es2017'
    },
    
    // Configure path aliases to match TypeScript configuration
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
        '@/components': resolve(__dirname, './src/components'),
        '@/components/payments': resolve(__dirname, './src/components/payments'),
        '@/api': resolve(__dirname, './src/api'),
        '@/api/payments': resolve(__dirname, './src/api/payments'),
        '@/redux': resolve(__dirname, './src/redux'),
        '@/redux/payments': resolve(__dirname, './src/redux/payments'),
        '@/app': resolve(__dirname, './src/app'),
        '@/i18n': resolve(__dirname, './src/i18n')
      }
    },
    
    // Configure CSS processing
    css: {
      // Enable PostCSS with Tailwind
      postcss: {
        plugins: [
          require('tailwindcss'),
          require('autoprefixer')
        ]
      },
      // Generate CSS modules for component styles
      modules: {
        localsConvention: 'camelCase'
      }
    },
    
    // Configure environment variables
    define: {
      // Make environment variables available to client code
      'process.env.NODE_ENV': JSON.stringify(mode),
      // Add payment-specific environment variables
      'process.env.PAYMENT_API_BASE': JSON.stringify(process.env.PAYMENT_API_BASE || '/api/organizations')
    },
    
    // Configure optimization settings
    optimizeDeps: {
      // Include dependencies that need optimization
      include: [
        'react',
        'react-dom',
        'react-router-dom',
        '@reduxjs/toolkit',
        'react-redux',
        '@tanstack/react-query'
      ],
      // Exclude dependencies that should not be optimized
      exclude: []
    },
    
    // Enable preview server for testing production builds
    preview: {
      port: 3001,
      open: true
    }
  };
});