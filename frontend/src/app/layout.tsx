import React from 'react';
import { Outlet } from 'react-router-dom';
import '@fontsource/geist-sans/400.css';
import '@fontsource/geist-sans/500.css';
import '@fontsource/geist-sans/600.css';
import '@fontsource/geist-sans/700.css';
import '@fontsource/geist-mono/400.css';
import '@fontsource/geist-mono/500.css';
import './globals.css';

/**
 * Root layout component that provides the basic HTML structure
 * and applies global styles and fonts. This component serves as
 * the main layout wrapper for all routes in the application.
 * 
 * This replaces the Next.js-specific layout with a React Router
 * compatible version that uses the Outlet component to render
 * nested routes.
 */
const RootLayout: React.FC = () => {
  return (
    <html lang="en">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="description" content="Core sample application" />
        <title>Core Sample Application</title>
      </head>
      <body className="antialiased">
        {/* 
          The Outlet component is a placeholder where React Router
          will render the matched child route component
        */}
        <Outlet />
      </body>
    </html>
  );
};

export default RootLayout;