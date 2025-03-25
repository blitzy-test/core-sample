import React from 'react';
import { Outlet, useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeftIcon, HomeIcon } from '@heroicons/react/24/outline';

/**
 * PaymentsLayout - Main container component for the payments module
 * 
 * This component serves as the layout wrapper for all payment-related pages.
 * It provides common navigation elements, breadcrumbs, and the container structure
 * for payment views. It uses React Router's Outlet to render nested payment routes.
 */
const PaymentsLayout: React.FC = () => {
  const navigate = useNavigate();
  const { orgId = '_all', accountId = '_all' } = useParams<{ orgId: string; accountId: string }>();
  
  // Fetch organization details if specific org is selected
  const { data: organization } = useQuery({
    queryKey: ['organization', orgId],
    queryFn: async () => {
      // Skip fetching for _all placeholder
      if (orgId === '_all') return { name: 'All Organizations' };
      
      // This would be replaced with actual API call in a real implementation
      return { id: orgId, name: `Organization ${orgId}` };
    },
    enabled: !!orgId && orgId !== '_all',
  });
  
  // Fetch account details if specific account is selected
  const { data: account } = useQuery({
    queryKey: ['account', accountId],
    queryFn: async () => {
      // Skip fetching for _all placeholder
      if (accountId === '_all') return { name: 'All Accounts' };
      
      // This would be replaced with actual API call in a real implementation
      return { id: accountId, name: `Account ${accountId}` };
    },
    enabled: !!accountId && accountId !== '_all',
  });

  // Handle navigation back to the main payments page
  const handleBackToPayments = () => {
    navigate('/payments');
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Payment header with navigation */}
      <header className="bg-white dark:bg-gray-800 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4 md:py-6">
            <div className="flex items-center">
              <button
                onClick={handleBackToPayments}
                className="mr-3 p-1 rounded-full text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                <ChevronLeftIcon className="h-5 w-5" aria-hidden="true" />
                <span className="sr-only">Back to Payments</span>
              </button>
              
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Payments</h1>
            </div>
            
            {/* Action buttons could be added here for global payment actions */}
          </div>
          
          {/* Breadcrumb navigation */}
          <div className="py-2 md:py-3">
            <nav className="flex" aria-label="Breadcrumb">
              <ol className="flex items-center space-x-2 text-sm text-gray-500 dark:text-gray-400">
                <li>
                  <Link to="/" className="hover:text-gray-700 dark:hover:text-gray-300 flex items-center">
                    <HomeIcon className="flex-shrink-0 h-4 w-4 mr-1" aria-hidden="true" />
                    <span>Home</span>
                  </Link>
                </li>
                <li className="flex items-center">
                  <svg className="flex-shrink-0 h-4 w-4 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                  </svg>
                  <Link to="/payments" className="ml-2 hover:text-gray-700 dark:hover:text-gray-300">
                    Payments
                  </Link>
                </li>
                
                {/* Conditional organization breadcrumb */}
                {orgId && orgId !== '_all' && (
                  <li className="flex items-center">
                    <svg className="flex-shrink-0 h-4 w-4 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                      <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                    </svg>
                    <Link 
                      to={`/payments/organizations/${orgId}`} 
                      className="ml-2 hover:text-gray-700 dark:hover:text-gray-300"
                    >
                      {organization?.name || `Organization ${orgId}`}
                    </Link>
                  </li>
                )}
                
                {/* Conditional account breadcrumb */}
                {accountId && accountId !== '_all' && (
                  <li className="flex items-center">
                    <svg className="flex-shrink-0 h-4 w-4 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                      <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                    </svg>
                    <Link 
                      to={`/payments/organizations/${orgId}/accounts/${accountId}`} 
                      className="ml-2 hover:text-gray-700 dark:hover:text-gray-300"
                    >
                      {account?.name || `Account ${accountId}`}
                    </Link>
                  </li>
                )}
              </ol>
            </nav>
          </div>
        </div>
      </header>
      
      {/* Main content area with responsive padding */}
      <main className="flex-grow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          {/* Render nested payment routes using React Router's Outlet */}
          <Outlet />
        </div>
      </main>
      
      {/* Footer */}
      <footer className="bg-white dark:bg-gray-800 shadow-inner">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="text-center text-sm text-gray-500 dark:text-gray-400">
            <p>Payment processing system &copy; {new Date().getFullYear()}</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default PaymentsLayout;