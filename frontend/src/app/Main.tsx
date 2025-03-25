import React, { useEffect, useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { Provider as ReduxProvider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { configureStore } from '@reduxjs/toolkit';
import { Geist, Geist_Mono } from '@fontsource/geist-sans';
import AppRoutes from './routes';

// Import reducers
// Note: These will be created in separate files
const rootReducer = {
  // Core reducers
  // Payment reducers will be added here when implemented
};

// Configure Redux store
const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore non-serializable values in specific action types if needed
        ignoredActions: [],
      },
    }),
  devTools: process.env.NODE_ENV !== 'production',
});

// Configure React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
    },
    mutations: {
      retry: 0,
    },
  },
});

// Define store type for TypeScript
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

/**
 * Main application component that serves as the entry point
 * Configures and provides all necessary context providers:
 * - Redux store for state management
 * - React Query for data fetching
 * - React Router for navigation
 */
const Main: React.FC = () => {
  // State to track theme preference
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  // Effect to detect and apply system theme preference
  useEffect(() => {
    // Check for system preference
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = prefersDark ? 'dark' : 'light';
    setTheme(initialTheme);
    
    // Apply theme to document
    document.documentElement.classList.toggle('dark', initialTheme === 'dark');
    
    // Listen for system theme changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = (e: MediaQueryListEvent) => {
      const newTheme = e.matches ? 'dark' : 'light';
      setTheme(newTheme);
      document.documentElement.classList.toggle('dark', newTheme === 'dark');
    };
    
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // Apply font classes to body
  useEffect(() => {
    // Add font classes to body
    document.body.classList.add('antialiased');
    
    // Clean up on unmount
    return () => {
      document.body.classList.remove('antialiased');
    };
  }, []);

  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <div className={`${theme === 'dark' ? 'dark' : ''}`}>
            <AppRoutes />
          </div>
          {process.env.NODE_ENV !== 'production' && <ReactQueryDevtools />}
        </BrowserRouter>
      </QueryClientProvider>
    </ReduxProvider>
  );
};

export default Main;