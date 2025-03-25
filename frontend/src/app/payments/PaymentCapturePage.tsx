import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';

// Types
interface PaymentTransaction {
  transaction_id: string;
  status: string;
  amount: number;
  currency: string;
  created_at: string;
  merchant_id: string;
  payment_type: string;
  transaction_reference?: string;
  description?: string;
}

interface CaptureFormData {
  captureType: 'full' | 'partial';
  amount: number;
  notes?: string;
}

interface CapturePaymentRequest {
  transaction_id: string;
  amount: number;
  notes?: string;
}

// API functions
const fetchTransaction = async (orgId: string, accountId: string, transactionId: string): Promise<PaymentTransaction> => {
  const response = await fetch(
    `/organizations/${orgId}/accounts/${accountId}/transactions/${transactionId}`
  );
  
  if (!response.ok) {
    throw new Error('Failed to fetch transaction details');
  }
  
  return response.json();
};

const capturePayment = async (
  orgId: string, 
  accountId: string, 
  data: CapturePaymentRequest
): Promise<any> => {
  const response = await fetch(
    `/organizations/${orgId}/accounts/${accountId}/transactions/${data.transaction_id}/capture`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        amount: data.amount,
        notes: data.notes || '',
      }),
    }
  );
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || 'Failed to capture payment');
  }
  
  return response.json();
};

// Component
const PaymentCapturePage: React.FC = () => {
  const { orgId, accountId, transactionId } = useParams<{ 
    orgId: string; 
    accountId: string; 
    transactionId: string;
  }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [formData, setFormData] = useState<CaptureFormData | null>(null);
  
  // Form setup with React Hook Form
  const { 
    control, 
    handleSubmit, 
    watch, 
    setValue, 
    formState: { errors } 
  } = useForm<CaptureFormData>({
    defaultValues: {
      captureType: 'full',
      amount: 0,
      notes: '',
    }
  });
  
  const captureType = watch('captureType');
  
  // Fetch transaction details
  const { 
    data: transaction, 
    isLoading, 
    isError, 
    error 
  } = useQuery({
    queryKey: ['transaction', orgId, accountId, transactionId],
    queryFn: () => fetchTransaction(orgId!, accountId!, transactionId!),
    enabled: !!orgId && !!accountId && !!transactionId,
    staleTime: 30000, // 30 seconds
  });
  
  // Update amount when transaction loads or capture type changes
  useEffect(() => {
    if (transaction && captureType === 'full') {
      setValue('amount', transaction.amount);
    }
  }, [transaction, captureType, setValue]);
  
  // Capture payment mutation
  const captureMutation = useMutation({
    mutationFn: (data: CapturePaymentRequest) => 
      capturePayment(orgId!, accountId!, data),
    onSuccess: () => {
      // Invalidate transaction queries to refresh data
      queryClient.invalidateQueries({ 
        queryKey: ['transaction', orgId, accountId, transactionId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['transactions', orgId, accountId] 
      });
      
      // Navigate back to transaction details
      navigate(`/payments/${orgId}/${accountId}/transactions/${transactionId}`);
    },
  });
  
  // Form submission handler
  const onSubmit = (data: CaptureFormData) => {
    setFormData(data);
    setShowConfirmation(true);
  };
  
  // Confirmation handler
  const handleConfirmCapture = () => {
    if (!formData || !transaction) return;
    
    captureMutation.mutate({
      transaction_id: transaction.transaction_id,
      amount: formData.amount,
      notes: formData.notes,
    });
  };
  
  // Format currency for display
  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };
  
  // Loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }
  
  // Error state
  if (isError || !transaction) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-500" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-red-700">
                {error instanceof Error ? error.message : 'Failed to load transaction details'}
              </p>
            </div>
          </div>
        </div>
        <Link 
          to={`/payments/${orgId}/${accountId}/transactions`}
          className="text-blue-600 hover:text-blue-800"
        >
          &larr; Back to Transactions
        </Link>
      </div>
    );
  }
  
  // Check if transaction is in a capturable state
  const isCapturable = transaction.status === 'AUTHORIZED';
  
  if (!isCapturable) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <div className="bg-yellow-50 border-l-4 border-yellow-500 p-4 mb-6">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-500" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-yellow-700">
                This transaction cannot be captured. Only transactions with status 'AUTHORIZED' can be captured.
              </p>
            </div>
          </div>
        </div>
        <Link 
          to={`/payments/${orgId}/${accountId}/transactions/${transactionId}`}
          className="text-blue-600 hover:text-blue-800"
        >
          &larr; Back to Transaction Details
        </Link>
      </div>
    );
  }
  
  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <Link 
          to={`/payments/${orgId}/${accountId}/transactions/${transactionId}`}
          className="text-blue-600 hover:text-blue-800"
        >
          &larr; Back to Transaction Details
        </Link>
        <h1 className="text-2xl font-bold mt-4">Capture Payment</h1>
      </div>
      
      {/* Transaction Summary */}
      <div className="bg-white shadow-md rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold mb-4">Transaction Summary</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-600">Transaction ID</p>
            <p className="font-medium">{transaction.transaction_id}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Status</p>
            <p className="font-medium">
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                {transaction.status}
              </span>
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Amount</p>
            <p className="font-medium">{formatCurrency(transaction.amount, transaction.currency)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Date Created</p>
            <p className="font-medium">
              {format(new Date(transaction.created_at), 'MMM dd, yyyy HH:mm:ss')}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Merchant ID</p>
            <p className="font-medium">{transaction.merchant_id}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Payment Type</p>
            <p className="font-medium">{transaction.payment_type}</p>
          </div>
          {transaction.transaction_reference && (
            <div>
              <p className="text-sm text-gray-600">Reference</p>
              <p className="font-medium">{transaction.transaction_reference}</p>
            </div>
          )}
        </div>
      </div>
      
      {/* Capture Form */}
      <div className="bg-white shadow-md rounded-lg p-6">
        <h2 className="text-lg font-semibold mb-4">Capture Details</h2>
        
        <form onSubmit={handleSubmit(onSubmit)}>
          {/* Capture Type */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Capture Type
            </label>
            <div className="space-y-2">
              <Controller
                name="captureType"
                control={control}
                render={({ field }) => (
                  <>
                    <div className="flex items-center">
                      <input
                        type="radio"
                        id="full-amount"
                        value="full"
                        checked={field.value === 'full'}
                        onChange={() => field.onChange('full')}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                      />
                      <label htmlFor="full-amount" className="ml-2 block text-sm text-gray-700">
                        Full Amount - {formatCurrency(transaction.amount, transaction.currency)}
                      </label>
                    </div>
                    <div className="flex items-center">
                      <input
                        type="radio"
                        id="partial-amount"
                        value="partial"
                        checked={field.value === 'partial'}
                        onChange={() => field.onChange('partial')}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
                      />
                      <label htmlFor="partial-amount" className="ml-2 block text-sm text-gray-700">
                        Partial Amount
                      </label>
                    </div>
                  </>
                )}
              />
            </div>
          </div>
          
          {/* Amount */}
          <div className="mb-4">
            <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-2">
              Capture Amount ({transaction.currency})
            </label>
            <Controller
              name="amount"
              control={control}
              rules={{
                required: 'Amount is required',
                min: {
                  value: 0.01,
                  message: 'Amount must be greater than 0',
                },
                max: {
                  value: transaction.amount,
                  message: `Amount cannot exceed ${formatCurrency(transaction.amount, transaction.currency)}`,
                },
                validate: {
                  isNumber: (value) => !isNaN(value) || 'Amount must be a valid number',
                }
              }}
              render={({ field }) => (
                <div>
                  <input
                    type="number"
                    id="amount"
                    step="0.01"
                    disabled={captureType === 'full'}
                    className={`mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm ${
                      captureType === 'full' ? 'bg-gray-100' : ''
                    }`}
                    {...field}
                    onChange={(e) => field.onChange(parseFloat(e.target.value))}
                  />
                  {errors.amount && (
                    <p className="mt-1 text-sm text-red-600">{errors.amount.message}</p>
                  )}
                </div>
              )}
            />
          </div>
          
          {/* Notes */}
          <div className="mb-6">
            <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-2">
              Notes (Optional)
            </label>
            <Controller
              name="notes"
              control={control}
              render={({ field }) => (
                <textarea
                  id="notes"
                  rows={3}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                  placeholder="Add any notes about this capture"
                  {...field}
                />
              )}
            />
          </div>
          
          {/* Submit Button */}
          <div className="flex justify-end">
            <Link
              to={`/payments/${orgId}/${accountId}/transactions/${transactionId}`}
              className="mr-4 inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Cancel
            </Link>
            <button
              type="submit"
              className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Capture Payment
            </button>
          </div>
        </form>
      </div>
      
      {/* Confirmation Modal */}
      {showConfirmation && formData && (
        <div className="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Confirm Capture</h3>
            <p className="text-sm text-gray-500 mb-4">
              Are you sure you want to capture this payment?
            </p>
            
            <div className="mb-4">
              <p className="text-sm text-gray-600">Transaction ID</p>
              <p className="font-medium">{transaction.transaction_id}</p>
            </div>
            
            <div className="mb-4">
              <p className="text-sm text-gray-600">Capture Amount</p>
              <p className="font-medium">{formatCurrency(formData.amount, transaction.currency)}</p>
            </div>
            
            {formData.notes && (
              <div className="mb-4">
                <p className="text-sm text-gray-600">Notes</p>
                <p className="font-medium">{formData.notes}</p>
              </div>
            )}
            
            <div className="mt-6 flex justify-end space-x-3">
              <button
                type="button"
                className="inline-flex justify-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                onClick={() => setShowConfirmation(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                onClick={handleConfirmCapture}
                disabled={captureMutation.isPending}
              >
                {captureMutation.isPending ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Processing...
                  </span>
                ) : (
                  'Confirm Capture'
                )}
              </button>
            </div>
            
            {/* Error message */}
            {captureMutation.isError && (
              <div className="mt-3 bg-red-50 border-l-4 border-red-500 p-4">
                <div className="flex">
                  <div className="flex-shrink-0">
                    <svg className="h-5 w-5 text-red-500" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div className="ml-3">
                    <p className="text-sm text-red-700">
                      {captureMutation.error instanceof Error 
                        ? captureMutation.error.message 
                        : 'Failed to capture payment'}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default PaymentCapturePage;