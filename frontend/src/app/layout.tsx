import { Outlet } from 'react-router-dom';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { store } from '../redux/store';
import './globals.css';

// Import fonts directly for Vite instead of using Next.js font loader
import '@fontsource/geist-sans';
import '@fontsource/geist-mono';

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1 minute
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

/**
 * Root layout component that wraps the entire application
 * Provides Redux store, React Query client, and React Router outlet
 */
export default function RootLayout() {
  return (
    <html lang="en">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta name="description" content="Core sample application with payments module" />
        <title>Core Sample Application</title>
      </head>
      <body className="font-geist-sans antialiased">
        {/* Redux Provider for global state management */}
        <Provider store={store}>
          {/* React Query Provider for data fetching */}
          <QueryClientProvider client={queryClient}>
            {/* React Router Outlet for rendering nested routes */}
            <Outlet />
          </QueryClientProvider>
        </Provider>
      </body>
    </html>
  );
}