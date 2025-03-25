import { lazy, Suspense } from 'react';
import { Navigate, RouteObject } from 'react-router-dom';

// Lazy-loaded components for code splitting
const Layout = lazy(() => import('./layout'));
const HomePage = lazy(() => import('./page'));

// Payment module components
const PaymentsLayout = lazy(() => import('./payments'));
const PaymentListPage = lazy(() => import('./payments/PaymentListPage'));
const PaymentDetailPage = lazy(() => import('./payments/PaymentDetailPage'));
const PaymentCapturePage = lazy(() => import('./payments/PaymentCapturePage'));
const PaymentRefundPage = lazy(() => import('./payments/PaymentRefundPage'));

// Loading fallback component
const LoadingFallback = () => (
  <div className="flex items-center justify-center min-h-screen">
    <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
  </div>
);

/**
 * Application route configuration
 * 
 * Defines the routing structure for the entire application using React Router v6.
 * Implements nested routes with lazy loading for code splitting and performance optimization.
 * Includes specialized routes for the payments module with proper parameter handling.
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <Layout />
      </Suspense>
    ),
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <HomePage />
          </Suspense>
        ),
      },
      // Payments module routes
      {
        path: 'organizations/:orgId/accounts/:accountId/payments',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <PaymentsLayout />
          </Suspense>
        ),
        children: [
          {
            index: true,
            element: (
              <Suspense fallback={<LoadingFallback />}>
                <PaymentListPage />
              </Suspense>
            ),
          },
          {
            path: 'transactions/:transactionId',
            element: (
              <Suspense fallback={<LoadingFallback />}>
                <PaymentDetailPage />
              </Suspense>
            ),
          },
          {
            path: 'transactions/:transactionId/capture',
            element: (
              <Suspense fallback={<LoadingFallback />}>
                <PaymentCapturePage />
              </Suspense>
            ),
          },
          {
            path: 'transactions/:transactionId/refund',
            element: (
              <Suspense fallback={<LoadingFallback />}>
                <PaymentRefundPage />
              </Suspense>
            ),
          },
        ],
      },
      // Special route for accessing all accounts within an organization
      {
        path: 'organizations/:orgId/payments',
        element: <Navigate to={`organizations/:orgId/accounts/_all/payments`} replace />,
      },
      // Special route for accessing all organizations
      {
        path: 'payments',
        element: <Navigate to="organizations/_all/accounts/_all/payments" replace />,
      },
      // Catch-all route for 404 handling
      {
        path: '*',
        element: (
          <div className="flex flex-col items-center justify-center min-h-screen p-4">
            <h1 className="text-2xl font-bold mb-4">Page Not Found</h1>
            <p className="mb-6">The page you are looking for does not exist.</p>
            <a 
              href="/"
              className="px-4 py-2 bg-primary text-white rounded hover:bg-primary-dark transition-colors"
            >
              Return Home
            </a>
          </div>
        ),
      },
    ],
  },
];

/**
 * Helper function to generate payment route paths
 * 
 * @param orgId - Organization ID or '_all' for all organizations
 * @param accountId - Account ID or '_all' for all accounts
 * @param transactionId - Optional transaction ID for detail views
 * @param action - Optional action (capture, refund) for transaction operations
 * @returns Formatted route path
 */
export const getPaymentRoutePath = (
  orgId: string = '_all',
  accountId: string = '_all',
  transactionId?: string,
  action?: 'capture' | 'refund'
): string => {
  let path = `/organizations/${orgId}/accounts/${accountId}/payments`;
  
  if (transactionId) {
    path += `/transactions/${transactionId}`;
    
    if (action) {
      path += `/${action}`;
    }
  }
  
  return path;
};

export default routes;