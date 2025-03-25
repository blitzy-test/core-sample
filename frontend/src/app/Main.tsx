import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { Provider as ReduxProvider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { configureStore } from '@reduxjs/toolkit';
import { Geist, Geist_Mono } from 'geist/font';
import AppRoutes from './routes';

// Import reducers
// Note: These will be created in separate files in the redux directory
// This is a placeholder for the store configuration
const store = configureStore({
  reducer: {
    // Core application reducers
    // Will be expanded as more features are added
    // payments: paymentsReducer will be imported from redux/payments
  },
  // Enable Redux DevTools in development
  devTools: process.env.NODE_ENV !== 'production',
});

// Configure React Query client with default options
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

// Define the root type for the Redux store
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

/**
 * Main application component that serves as the entry point
 * Configures and provides all necessary context providers:
 * - Redux store for global state management
 * - React Query for data fetching and caching
 * - React Router for navigation
 */
function Main() {
  // Detect user's preferred color scheme
  const prefersDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  // Apply theme class to body
  React.useEffect(() => {
    document.body.classList.toggle('dark', prefersDarkMode);
    
    // Add font classes from Geist
    document.body.classList.add(Geist.variable);
    document.body.classList.add(Geist_Mono.variable);
    document.body.classList.add('antialiased');
    
    // Listen for changes in color scheme preference
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = (e: MediaQueryListEvent) => {
      document.body.classList.toggle('dark', e.matches);
    };
    
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);
  
  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
        {/* Enable React Query DevTools in development */}
        {process.env.NODE_ENV !== 'production' && <ReactQueryDevtools initialIsOpen={false} />}
      </QueryClientProvider>
    </ReduxProvider>
  );
}

export default Main;