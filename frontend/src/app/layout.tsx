import React from "react";
import { Outlet } from "react-router-dom";
import { Provider } from "react-redux";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./globals.css";

// Import Geist fonts using Vite's approach
import "@fontsource/geist-sans/400.css";
import "@fontsource/geist-sans/500.css";
import "@fontsource/geist-sans/600.css";
import "@fontsource/geist-sans/700.css";
import "@fontsource/geist-mono/400.css";
import "@fontsource/geist-mono/500.css";

// Import store from Redux store file
import { store } from "../redux/store";

// Create a React Query client
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
 * Provides Redux store and React Query client to all child components
 * Sets up global styling and font configuration
 */
function RootLayout() {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <html lang="en">
          <head>
            <meta charSet="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Core Sample Application</title>
            <meta name="description" content="Core sample application with payments module" />
          </head>
          <body className="antialiased">
            <Outlet />
          </body>
        </html>
      </QueryClientProvider>
    </Provider>
  );
}

export default RootLayout;