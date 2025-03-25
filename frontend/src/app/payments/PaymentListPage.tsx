import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';

// Types
interface PaymentTransaction {
  transaction_id: string;
  organization_id: string;
  account_id: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  created_at: string;
  updated_at: string;
  merchant_id: string;
  payment_type: string;
  transaction_reference?: string;
  description?: string;
}

enum PaymentStatus {
  CREATED = 'CREATED',
  AUTHORIZED = 'AUTHORIZED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  CAPTURED = 'CAPTURED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
  PARTIALLY_REFUNDED = 'PARTIALLY_REFUNDED',
  VOIDED = 'VOIDED'
}

interface PaymentListResponse {
  transactions: PaymentTransaction[];
  total_count: number;
  page_size: number;
  page_number: number;
  total_pages: number;
}

interface FilterState {
  fromDate: string;
  toDate: string;
  status: PaymentStatus[];
  minAmount: string;
  maxAmount: string;
  merchantId: string;
  paymentType: string;
}

// Fetch payments function
const fetchPayments = async (
  orgId: string,
  accountId: string,
  pageNumber: number,
  pageSize: number,
  sortBy: string,
  sortDirection: string,
  filters: FilterState
): Promise<PaymentListResponse> => {
  // Build query parameters
  const params = new URLSearchParams();
  params.append('pageNumber', pageNumber.toString());
  params.append('pageSize', pageSize.toString());
  params.append('sortBy', sortBy);
  params.append('sortDirection', sortDirection);
  
  // Add filters to query params
  if (filters.fromDate) params.append('fromDate', filters.fromDate);
  if (filters.toDate) params.append('toDate', filters.toDate);
  if (filters.status.length > 0) {
    filters.status.forEach(status => params.append('statusIn', status));
  }
  if (filters.minAmount) params.append('minAmount', filters.minAmount);
  if (filters.maxAmount) params.append('maxAmount', filters.maxAmount);
  if (filters.merchantId) params.append('merchantId', filters.merchantId);
  if (filters.paymentType) params.append('paymentType', filters.paymentType);

  // Use _all placeholder for all accounts if accountId is not specified
  const accountPath = accountId || '_all';
  
  // Make API request
  const response = await fetch(
    `/api/v1/organizations/${orgId}/accounts/${accountPath}/transactions?${params.toString()}`,
    {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error('Failed to fetch payment transactions');
  }

  return response.json();
};

// Status badge component
const StatusBadge: React.FC<{ status: PaymentStatus }> = ({ status }) => {
  const getStatusStyles = () => {
    switch (status) {
      case PaymentStatus.COMPLETED:
      case PaymentStatus.CAPTURED:
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case PaymentStatus.PROCESSING:
      case PaymentStatus.AUTHORIZED:
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case PaymentStatus.FAILED:
      case PaymentStatus.VOIDED:
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case PaymentStatus.REFUNDED:
      case PaymentStatus.PARTIALLY_REFUNDED:
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200';
    }
  };

  return (
    <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusStyles()}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
};

// Format currency
const formatCurrency = (amount: number, currency: string): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(amount);
};

// Payment List Page Component
const PaymentListPage: React.FC = () => {
  // Get organization and account IDs from URL or context
  // In a real app, these would come from route params or auth context
  const orgId = 'org-123'; // Example value
  const accountId = 'acc-456'; // Example value
  
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  
  // Pagination state
  const [pageNumber, setPageNumber] = useState(parseInt(searchParams.get('page') || '1', 10));
  const [pageSize, setPageSize] = useState(parseInt(searchParams.get('pageSize') || '20', 10));
  
  // Sorting state
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'created_at');
  const [sortDirection, setSortDirection] = useState(searchParams.get('sortDirection') || 'desc');
  
  // Filter state
  const [filters, setFilters] = useState<FilterState>({
    fromDate: searchParams.get('fromDate') || '',
    toDate: searchParams.get('toDate') || '',
    status: (searchParams.getAll('statusIn') as PaymentStatus[]) || [],
    minAmount: searchParams.get('minAmount') || '',
    maxAmount: searchParams.get('maxAmount') || '',
    merchantId: searchParams.get('merchantId') || '',
    paymentType: searchParams.get('paymentType') || ''
  });
  
  // Filter panel visibility (for mobile)
  const [showFilters, setShowFilters] = useState(false);
  
  // Form state for filters
  const [filterForm, setFilterForm] = useState<FilterState>(filters);
  
  // Fetch payments data
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['payments', orgId, accountId, pageNumber, pageSize, sortBy, sortDirection, filters],
    queryFn: () => fetchPayments(orgId, accountId, pageNumber, pageSize, sortBy, sortDirection, filters),
    keepPreviousData: true
  });
  
  // Update URL when pagination, sorting, or filters change
  useEffect(() => {
    const params = new URLSearchParams();
    
    // Add pagination params
    params.set('page', pageNumber.toString());
    params.set('pageSize', pageSize.toString());
    
    // Add sorting params
    params.set('sortBy', sortBy);
    params.set('sortDirection', sortDirection);
    
    // Add filter params
    if (filters.fromDate) params.set('fromDate', filters.fromDate);
    if (filters.toDate) params.set('toDate', filters.toDate);
    filters.status.forEach(status => params.append('statusIn', status));
    if (filters.minAmount) params.set('minAmount', filters.minAmount);
    if (filters.maxAmount) params.set('maxAmount', filters.maxAmount);
    if (filters.merchantId) params.set('merchantId', filters.merchantId);
    if (filters.paymentType) params.set('paymentType', filters.paymentType);
    
    setSearchParams(params);
  }, [pageNumber, pageSize, sortBy, sortDirection, filters, setSearchParams]);
  
  // Handle sort change
  const handleSortChange = (column: string) => {
    if (sortBy === column) {
      // Toggle direction if same column
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // Set new column and default to descending
      setSortBy(column);
      setSortDirection('desc');
    }
    // Reset to first page when sorting changes
    setPageNumber(1);
  };
  
  // Handle filter apply
  const handleApplyFilters = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters(filterForm);
    setPageNumber(1); // Reset to first page when filters change
    setShowFilters(false); // Hide filter panel on mobile after applying
  };
  
  // Handle filter change
  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilterForm(prev => ({ ...prev, [name]: value }));
  };
  
  // Handle status filter change
  const handleStatusChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value, checked } = e.target;
    const status = value as PaymentStatus;
    
    setFilterForm(prev => {
      if (checked) {
        return { ...prev, status: [...prev.status, status] };
      } else {
        return { ...prev, status: prev.status.filter(s => s !== status) };
      }
    });
  };
  
  // Handle reset filters
  const handleResetFilters = () => {
    const emptyFilters: FilterState = {
      fromDate: '',
      toDate: '',
      status: [],
      minAmount: '',
      maxAmount: '',
      merchantId: '',
      paymentType: ''
    };
    setFilterForm(emptyFilters);
    setFilters(emptyFilters);
    setPageNumber(1);
  };
  
  // Navigate to transaction detail
  const handleViewTransaction = (transactionId: string) => {
    navigate(`/payments/${orgId}/${accountId}/transactions/${transactionId}`);
  };
  
  // Render sort indicator
  const renderSortIndicator = (column: string) => {
    if (sortBy !== column) return null;
    
    return (
      <span className="ml-1">
        {sortDirection === 'asc' ? '↑' : '↓'}
      </span>
    );
  };

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-semibold">Payment Transactions</h1>
        <div className="flex space-x-2">
          <button 
            className="md:hidden px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded-md"
            onClick={() => setShowFilters(!showFilters)}
          >
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </button>
          <button 
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            onClick={() => navigate(`/payments/${orgId}/${accountId}/transactions/new`)}
          >
            + New Payment
          </button>
        </div>
      </div>
      
      <div className="flex flex-col md:flex-row gap-6">
        {/* Filter Panel - Hidden on mobile unless toggled */}
        <div className={`${showFilters ? 'block' : 'hidden'} md:block w-full md:w-1/4 bg-white dark:bg-gray-800 p-4 rounded-lg shadow`}>
          <h2 className="text-lg font-medium mb-4">Filters</h2>
          <form onSubmit={handleApplyFilters}>
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Date Range</label>
              <div className="space-y-2">
                <div>
                  <label className="block text-xs mb-1">From</label>
                  <input
                    type="date"
                    name="fromDate"
                    value={filterForm.fromDate}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
                  />
                </div>
                <div>
                  <label className="block text-xs mb-1">To</label>
                  <input
                    type="date"
                    name="toDate"
                    value={filterForm.toDate}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
                  />
                </div>
              </div>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Status</label>
              <div className="space-y-1">
                {Object.values(PaymentStatus).map(status => (
                  <div key={status} className="flex items-center">
                    <input
                      type="checkbox"
                      id={`status-${status}`}
                      name="status"
                      value={status}
                      checked={filterForm.status.includes(status)}
                      onChange={handleStatusChange}
                      className="mr-2"
                    />
                    <label htmlFor={`status-${status}`} className="text-sm">
                      {status.replace(/_/g, ' ')}
                    </label>
                  </div>
                ))}
              </div>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Amount</label>
              <div className="space-y-2">
                <div>
                  <label className="block text-xs mb-1">Min</label>
                  <input
                    type="number"
                    name="minAmount"
                    value={filterForm.minAmount}
                    onChange={handleFilterChange}
                    placeholder="0.00"
                    min="0"
                    step="0.01"
                    className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
                  />
                </div>
                <div>
                  <label className="block text-xs mb-1">Max</label>
                  <input
                    type="number"
                    name="maxAmount"
                    value={filterForm.maxAmount}
                    onChange={handleFilterChange}
                    placeholder="0.00"
                    min="0"
                    step="0.01"
                    className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
                  />
                </div>
              </div>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Merchant</label>
              <select
                name="merchantId"
                value={filterForm.merchantId}
                onChange={handleFilterChange}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
              >
                <option value="">All Merchants</option>
                <option value="merchant-1">ACME Store</option>
                <option value="merchant-2">Example Shop</option>
                <option value="merchant-3">Test Merchant</option>
              </select>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Payment Type</label>
              <select
                name="paymentType"
                value={filterForm.paymentType}
                onChange={handleFilterChange}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
              >
                <option value="">All Types</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="DEBIT_CARD">Debit Card</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="DIGITAL_WALLET">Digital Wallet</option>
              </select>
            </div>
            
            <div className="flex space-x-2">
              <button
                type="submit"
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 flex-1"
              >
                Apply Filters
              </button>
              <button
                type="button"
                onClick={handleResetFilters}
                className="px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded-md hover:bg-gray-300 dark:hover:bg-gray-600"
              >
                Reset
              </button>
            </div>
          </form>
        </div>
        
        {/* Transactions List */}
        <div className="w-full md:w-3/4">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            {isLoading ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                <p className="mt-4">Loading transactions...</p>
              </div>
            ) : isError ? (
              <div className="p-8 text-center text-red-600 dark:text-red-400">
                <p>Error loading transactions: {(error as Error).message}</p>
                <button 
                  onClick={() => refetch()} 
                  className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  Retry
                </button>
              </div>
            ) : (
              <>
                {/* Transactions Table */}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead className="bg-gray-50 dark:bg-gray-700">
                      <tr>
                        <th 
                          scope="col" 
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer"
                          onClick={() => handleSortChange('transaction_id')}
                        >
                          ID {renderSortIndicator('transaction_id')}
                        </th>
                        <th 
                          scope="col" 
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer"
                          onClick={() => handleSortChange('created_at')}
                        >
                          Date {renderSortIndicator('created_at')}
                        </th>
                        <th 
                          scope="col" 
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer"
                          onClick={() => handleSortChange('merchant_id')}
                        >
                          Merchant {renderSortIndicator('merchant_id')}
                        </th>
                        <th 
                          scope="col" 
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer"
                          onClick={() => handleSortChange('amount')}
                        >
                          Amount {renderSortIndicator('amount')}
                        </th>
                        <th 
                          scope="col" 
                          className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer"
                          onClick={() => handleSortChange('status')}
                        >
                          Status {renderSortIndicator('status')}
                        </th>
                        <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                      {data?.transactions.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                            No transactions found matching your criteria.
                          </td>
                        </tr>
                      ) : (
                        data?.transactions.map((transaction) => (
                          <tr 
                            key={transaction.transaction_id} 
                            className="hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer"
                            onClick={() => handleViewTransaction(transaction.transaction_id)}
                          >
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                              {transaction.transaction_id.substring(0, 8)}...
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm">
                              {format(new Date(transaction.created_at), 'MMM dd, yyyy HH:mm')}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm">
                              {transaction.merchant_id}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm">
                              {formatCurrency(transaction.amount, transaction.currency)}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <StatusBadge status={transaction.status} />
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                              <button 
                                className="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleViewTransaction(transaction.transaction_id);
                                }}
                              >
                                View
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
                
                {/* Pagination */}
                {data && data.total_pages > 0 && (
                  <div className="px-6 py-4 bg-gray-50 dark:bg-gray-700 border-t border-gray-200 dark:border-gray-600 flex items-center justify-between">
                    <div className="flex items-center">
                      <span className="text-sm text-gray-700 dark:text-gray-300">
                        Showing {((pageNumber - 1) * pageSize) + 1} to {Math.min(pageNumber * pageSize, data.total_count)} of {data.total_count} results
                      </span>
                      <select
                        className="ml-4 px-2 py-1 border rounded-md dark:bg-gray-800 dark:border-gray-600"
                        value={pageSize}
                        onChange={(e) => {
                          setPageSize(Number(e.target.value));
                          setPageNumber(1); // Reset to first page when changing page size
                        }}
                      >
                        <option value="10">10 per page</option>
                        <option value="20">20 per page</option>
                        <option value="50">50 per page</option>
                        <option value="100">100 per page</option>
                      </select>
                    </div>
                    
                    <div className="flex space-x-2">
                      <button
                        className="px-3 py-1 border rounded-md dark:bg-gray-800 dark:border-gray-600 disabled:opacity-50"
                        disabled={pageNumber <= 1}
                        onClick={() => setPageNumber(prev => Math.max(prev - 1, 1))}
                      >
                        Previous
                      </button>
                      
                      <div className="flex space-x-1">
                        {Array.from({ length: Math.min(5, data.total_pages) }, (_, i) => {
                          // Show pages around current page
                          let pageToShow = pageNumber - 2 + i;
                          
                          // Adjust if we're at the beginning
                          if (pageNumber < 3) {
                            pageToShow = i + 1;
                          }
                          
                          // Adjust if we're at the end
                          if (pageNumber > data.total_pages - 2) {
                            pageToShow = data.total_pages - 4 + i;
                          }
                          
                          // Ensure page is in valid range
                          if (pageToShow > 0 && pageToShow <= data.total_pages) {
                            return (
                              <button
                                key={pageToShow}
                                className={`px-3 py-1 border rounded-md ${
                                  pageNumber === pageToShow
                                    ? 'bg-blue-600 text-white'
                                    : 'dark:bg-gray-800 dark:border-gray-600'
                                }`}
                                onClick={() => setPageNumber(pageToShow)}
                              >
                                {pageToShow}
                              </button>
                            );
                          }
                          return null;
                        })}
                      </div>
                      
                      <button
                        className="px-3 py-1 border rounded-md dark:bg-gray-800 dark:border-gray-600 disabled:opacity-50"
                        disabled={pageNumber >= data.total_pages}
                        onClick={() => setPageNumber(prev => Math.min(prev + 1, data.total_pages))}
                      >
                        Next
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PaymentListPage;