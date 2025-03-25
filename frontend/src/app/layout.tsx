import React from 'react';
import { Outlet } from 'react-router-dom';
import './globals.css';

/**
 * Root layout component for the application
 * 
 * This component serves as the main layout wrapper for all routes in the application.
 * It provides the basic HTML structure and applies global styles.
 * 
 * Note: Font loading is handled in the Main.tsx component to work with Vite instead of Next.js
 */
const RootLayout: React.FC = () => {
  return (
    <html lang="en">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta name="description" content="Core sample application with payments module" />
        <title>Core Sample Application</title>
      </head>
      <body>
        {/* 
          The Outlet component is a placeholder where child routes will be rendered
          This replaces the {children} prop from Next.js layouts
        */}
        <Outlet />
      </body>
    </html>
  );
};

export default RootLayout;