import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { Dialog } from '@headlessui/react';
import { ExclamationTriangleIcon, ArrowLeftIcon, CreditCardIcon, CheckCircleIcon } from '@heroicons/react/24/outline';

// Types
interface RefundFormData {
  amount: number;
  reason: string;
  refundFull: boolean;
}

interface Transaction {
  transaction_id: string;
  organization_id: string;
  account_id: string;
  status: string;
  amount: number;
  currency: string;
  created_at: string;
  updated_at: string;
  merchant_id: string;
  payment_type: string;
  transaction_reference?: string;
  description?: string;
  captured_amount?: number;
  refunded_amount?: number;
}

interface RefundRequest {
  transaction_id: string;
  amount: number;
  reason: string;
}

// API functions
const fetchTransaction = async (
  organizationId: string,
  accountId: string,
  transactionId: string
): Promise<Transaction> => {
  const response = await fetch(
    `/organizations/${organizationId}/accounts/${accountId}/transactions/${transactionId}`
  );
  
  if (!response.ok) {
    throw new Error('Failed to fetch transaction details');
  }
  
  return response.json();
};

const submitRefund = async (
  organizationId: string,
  accountId: string,
  refundData: RefundRequest
): Promise<any> => {
  const response = await fetch(
    `/organizations/${organizationId}/accounts/${accountId}/transactions/${refundData.transaction_id}/refund`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        amount: refundData.amount,
        reason: refundData.reason,
      }),
    }
  );
  
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.error?.message || 'Failed to process refund');
  }
  
  return response.json();
};

// Helper functions
const formatCurrency = (amount: number, currency: string): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(amount);
};

const hasRefundPermission = (): boolean => {
  // In a real implementation, this would check the user's permissions
  // from a context or Redux store
  return true;
};

/**
 * PaymentRefundPage Component
 * 
 * This component implements the payment refund workflow that allows users to process
 * refunds for completed transactions. It includes a form for refund details, amount
 * validation ensuring refund amounts don't exceed the original payment, confirmation
 * steps for this critical financial operation, and appropriate security measures.
 */
const PaymentRefundPage: React.FC = () => {
  const { organizationId, accountId, transactionId } = useParams<{
    organizationId: string;
    accountId: string;
    transactionId: string;
  }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);
  const [isSecurityDialogOpen, setIsSecurityDialogOpen] = useState(false);
  const [securityPassword, setSecurityPassword] = useState('');
  const [refundData, setRefundData] = useState<RefundFormData | null>(null);
  const [refundError, setRefundError] = useState<string | null>(null);
  
  // Form setup with React Hook Form
  const { 
    control, 
    handleSubmit, 
    watch, 
    setValue, 
    formState: { errors } 
  } = useForm<RefundFormData>({
    defaultValues: {
      amount: 0,
      reason: '',
      refundFull: false,
    },
  });
  
  const refundFull = watch('refundFull');
  
  // Fetch transaction details
  const { data: transaction, isLoading, error } = useQuery({
    queryKey: ['transaction', organizationId, accountId, transactionId],
    queryFn: () => fetchTransaction(organizationId!, accountId!, transactionId!),
    enabled: !!organizationId && !!accountId && !!transactionId,
  });
  
  // Calculate maximum refundable amount
  const maxRefundableAmount = transaction ? 
    (transaction.captured_amount || transaction.amount) - (transaction.refunded_amount || 0) : 
    0;
  
  // Update amount when "refund full" is toggled
  useEffect(() => {
    if (refundFull && transaction) {
      setValue('amount', maxRefundableAmount);
    }
  }, [refundFull, transaction, setValue, maxRefundableAmount]);
  
  // Refund mutation
  const refundMutation = useMutation({
    mutationFn: (data: RefundRequest) => 
      submitRefund(organizationId!, accountId!, data),
    onSuccess: () => {
      // Invalidate and refetch transaction data
      queryClient.invalidateQueries({
        queryKey: ['transaction', organizationId, accountId, transactionId]
      });
      
      // Navigate back to transaction details
      navigate(`/organizations/${organizationId}/accounts/${accountId}/transactions/${transactionId}`);
    },
    onError: (error: Error) => {
      setRefundError(error.message);
      setIsSecurityDialogOpen(false);
      setIsConfirmDialogOpen(false);
    },
  });
  
  // Form submission handler
  const onSubmit = (data: RefundFormData) => {
    // Store form data for confirmation dialog
    setRefundData(data);
    setIsConfirmDialogOpen(true);
  };
  
  // Confirm refund handler
  const handleConfirmRefund = () => {
    // For high-value refunds, require additional security verification
    if (refundData && refundData.amount >= 1000) {
      setIsConfirmDialogOpen(false);
      setIsSecurityDialogOpen(true);
    } else {
      processRefund();
    }
  };
  
  // Security verification handler
  const handleSecurityVerification = () => {
    // In a real implementation, this would validate the password
    // against the user's credentials
    if (securityPassword) {
      processRefund();
    }
  };
  
  // Process the refund after all confirmations
  const processRefund = () => {
    if (!refundData || !transaction) return;
    
    refundMutation.mutate({
      transaction_id: transaction.transaction_id,
      amount: refundData.amount,
      reason: refundData.reason,
    });
    
    setIsSecurityDialogOpen(false);
  };
  
  // Check if user has permission to refund
  if (!hasRefundPermission()) {
    return (
      <div className="p-6 bg-red-50 border border-red-200 rounded-lg">
        <h2 className="text-lg font-semibold text-red-700">Permission Denied</h2>
        <p className="mt-2 text-red-600">
          You do not have permission to process refunds. Please contact your administrator.
        </p>
        <button
          className="mt-4 flex items-center text-sm font-medium text-red-600 hover:text-red-800"
          onClick={() => navigate(-1)}
        >
          <ArrowLeftIcon className="w-4 h-4 mr-1" /> Return to previous page
        </button>
      </div>
    );
  }
  
  // Loading state
  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
      </div>
    );
  }
  
  // Error state
  if (error || !transaction) {
    return (
      <div className="p-6 bg-red-50 border border-red-200 rounded-lg">
        <h2 className="text-lg font-semibold text-red-700">Error Loading Transaction</h2>
        <p className="mt-2 text-red-600">
          {error instanceof Error ? error.message : 'Failed to load transaction details'}
        </p>
        <button
          className="mt-4 flex items-center text-sm font-medium text-red-600 hover:text-red-800"
          onClick={() => navigate(-1)}
        >
          <ArrowLeftIcon className="w-4 h-4 mr-1" /> Return to previous page
        </button>
      </div>
    );
  }
  
  // Check if transaction can be refunded
  const canBeRefunded = transaction.status === 'captured' || transaction.status === 'settled';
  if (!canBeRefunded) {
    return (
      <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-lg">
        <h2 className="text-lg font-semibold text-yellow-700">Refund Not Available</h2>
        <p className="mt-2 text-yellow-600">
          This transaction cannot be refunded because it is in {transaction.status} status.
          Only captured or settled transactions can be refunded.
        </p>
        <button
          className="mt-4 flex items-center text-sm font-medium text-yellow-600 hover:text-yellow-800"
          onClick={() => navigate(-1)}
        >
          <ArrowLeftIcon className="w-4 h-4 mr-1" /> Return to transaction details
        </button>
      </div>
    );
  }
  
  // Check if transaction has already been fully refunded
  if (maxRefundableAmount <= 0) {
    return (
      <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-lg">
        <h2 className="text-lg font-semibold text-yellow-700">Fully Refunded</h2>
        <p className="mt-2 text-yellow-600">
          This transaction has already been fully refunded and no further refunds can be processed.
        </p>
        <button
          className="mt-4 flex items-center text-sm font-medium text-yellow-600 hover:text-yellow-800"
          onClick={() => navigate(-1)}
        >
          <ArrowLeftIcon className="w-4 h-4 mr-1" /> Return to transaction details
        </button>
      </div>
    );
  }
  
  return (
    <div className="max-w-4xl mx-auto px-4 py-6">
      {/* Header */}
      <div className="mb-6">
        <button
          className="flex items-center text-sm font-medium text-gray-600 hover:text-gray-800"
          onClick={() => navigate(-1)}
        >
          <ArrowLeftIcon className="w-4 h-4 mr-1" /> Back to transaction
        </button>
        <h1 className="mt-4 text-2xl font-bold text-gray-900">Process Refund</h1>
      </div>
      
      {/* Transaction Summary Card */}
      <div className="mb-8 bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-800">Transaction Summary</h2>
        </div>
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm font-medium text-gray-500">Transaction ID</p>
            <p className="mt-1 font-mono text-gray-900">{transaction.transaction_id}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Merchant</p>
            <p className="mt-1 text-gray-900">{transaction.merchant_id}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Original Amount</p>
            <p className="mt-1 text-gray-900 font-semibold">
              {formatCurrency(transaction.amount, transaction.currency)}
            </p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Date</p>
            <p className="mt-1 text-gray-900">
              {format(new Date(transaction.created_at), 'MMM d, yyyy h:mm a')}
            </p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Status</p>
            <p className="mt-1">
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                {transaction.status}
              </span>
            </p>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Payment Type</p>
            <p className="mt-1 text-gray-900">{transaction.payment_type}</p>
          </div>
          {transaction.refunded_amount > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-500">Previously Refunded</p>
              <p className="mt-1 text-orange-600 font-semibold">
                {formatCurrency(transaction.refunded_amount, transaction.currency)}
              </p>
            </div>
          )}
          <div>
            <p className="text-sm font-medium text-gray-500">Available for Refund</p>
            <p className="mt-1 text-blue-600 font-semibold">
              {formatCurrency(maxRefundableAmount, transaction.currency)}
            </p>
          </div>
        </div>
      </div>
      
      {/* Refund Form */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-800">Refund Details</h2>
        </div>
        <div className="p-6">
          {refundError && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
              <div className="flex">
                <ExclamationTriangleIcon className="h-5 w-5 text-red-400" aria-hidden="true" />
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-red-800">Refund Error</h3>
                  <p className="text-sm text-red-700 mt-1">{refundError}</p>
                </div>
              </div>
            </div>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-6">
              {/* Refund Type Selection */}
              <div>
                <label className="flex items-center">
                  <Controller
                    name="refundFull"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="checkbox"
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    )}
                  />
                  <span className="ml-2 text-sm font-medium text-gray-700">
                    Refund full amount ({formatCurrency(maxRefundableAmount, transaction.currency)})
                  </span>
                </label>
              </div>
              
              {/* Refund Amount */}
              <div>
                <label htmlFor="amount" className="block text-sm font-medium text-gray-700">
                  Refund Amount
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <span className="text-gray-500 sm:text-sm">{transaction.currency === 'USD' ? '$' : transaction.currency}</span>
                  </div>
                  <Controller
                    name="amount"
                    control={control}
                    rules={{
                      required: 'Refund amount is required',
                      min: {
                        value: 0.01,
                        message: 'Amount must be greater than 0',
                      },
                      max: {
                        value: maxRefundableAmount,
                        message: `Amount cannot exceed ${formatCurrency(maxRefundableAmount, transaction.currency)}`,
                      },
                      validate: {
                        isNumber: (value) => !isNaN(value) || 'Please enter a valid number',
                      },
                    }}
                    render={({ field }) => (
                      <input
                        type="number"
                        id="amount"
                        className={`block w-full pl-10 pr-12 py-2 sm:text-sm border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 ${
                          errors.amount ? 'border-red-300' : ''
                        }`}
                        placeholder="0.00"
                        step="0.01"
                        disabled={refundFull}
                        {...field}
                        onChange={(e) => field.onChange(parseFloat(e.target.value) || 0)}
                      />
                    )}
                  />
                  <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                    <span className="text-gray-500 sm:text-sm">{transaction.currency}</span>
                  </div>
                </div>
                {errors.amount && (
                  <p className="mt-2 text-sm text-red-600">{errors.amount.message}</p>
                )}
              </div>
              
              {/* Refund Reason */}
              <div>
                <label htmlFor="reason" className="block text-sm font-medium text-gray-700">
                  Reason for Refund
                </label>
                <div className="mt-1">
                  <Controller
                    name="reason"
                    control={control}
                    rules={{
                      required: 'Refund reason is required',
                      minLength: {
                        value: 5,
                        message: 'Reason must be at least 5 characters',
                      },
                    }}
                    render={({ field }) => (
                      <textarea
                        id="reason"
                        rows={3}
                        className={`shadow-sm focus:ring-blue-500 focus:border-blue-500 block w-full sm:text-sm border-gray-300 rounded-md ${
                          errors.reason ? 'border-red-300' : ''
                        }`}
                        placeholder="Please provide a reason for this refund"
                        {...field}
                      />
                    )}
                  />
                </div>
                {errors.reason && (
                  <p className="mt-2 text-sm text-red-600">{errors.reason.message}</p>
                )}
              </div>
              
              {/* Submit Button */}
              <div className="flex justify-end">
                <button
                  type="button"
                  className="mr-4 px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  onClick={() => navigate(-1)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  disabled={refundMutation.isPending}
                >
                  {refundMutation.isPending ? 'Processing...' : 'Process Refund'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
      
      {/* Confirmation Dialog */}
      <Dialog
        open={isConfirmDialogOpen}
        onClose={() => setIsConfirmDialogOpen(false)}
        className="relative z-50"
      >
        <div className="fixed inset-0 bg-black/30" aria-hidden="true" />
        
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <Dialog.Panel className="mx-auto max-w-md rounded-lg bg-white p-6 shadow-xl">
            <Dialog.Title className="text-lg font-medium text-gray-900">
              Confirm Refund
            </Dialog.Title>
            
            <div className="mt-4">
              <p className="text-sm text-gray-500">
                You are about to process a refund for:
              </p>
              <div className="mt-2 p-4 bg-gray-50 rounded-md">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium text-gray-700">Amount:</span>
                  <span className="text-sm font-bold text-gray-900">
                    {refundData && formatCurrency(refundData.amount, transaction.currency)}
                  </span>
                </div>
                <div className="mt-2 flex justify-between items-center">
                  <span className="text-sm font-medium text-gray-700">Transaction ID:</span>
                  <span className="text-sm font-mono text-gray-900">
                    {transaction.transaction_id.substring(0, 12)}...
                  </span>
                </div>
              </div>
              <p className="mt-4 text-sm text-red-600 font-medium">
                This action cannot be undone. Are you sure you want to continue?
              </p>
            </div>
            
            <div className="mt-6 flex justify-end space-x-4">
              <button
                type="button"
                className="px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                onClick={() => setIsConfirmDialogOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                onClick={handleConfirmRefund}
              >
                Confirm Refund
              </button>
            </div>
          </Dialog.Panel>
        </div>
      </Dialog>
      
      {/* Security Verification Dialog */}
      <Dialog
        open={isSecurityDialogOpen}
        onClose={() => setIsSecurityDialogOpen(false)}
        className="relative z-50"
      >
        <div className="fixed inset-0 bg-black/30" aria-hidden="true" />
        
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <Dialog.Panel className="mx-auto max-w-md rounded-lg bg-white p-6 shadow-xl">
            <Dialog.Title className="text-lg font-medium text-gray-900 flex items-center">
              <ExclamationTriangleIcon className="h-5 w-5 text-yellow-500 mr-2" />
              Security Verification Required
            </Dialog.Title>
            
            <div className="mt-4">
              <p className="text-sm text-gray-500">
                This is a high-value refund and requires additional security verification.
                Please enter your password to confirm this action.
              </p>
              
              <div className="mt-4">
                <label htmlFor="security-password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <input
                  type="password"
                  id="security-password"
                  className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  value={securityPassword}
                  onChange={(e) => setSecurityPassword(e.target.value)}
                />
              </div>
              
              <div className="mt-4 p-4 bg-yellow-50 rounded-md">
                <div className="flex">
                  <ExclamationTriangleIcon className="h-5 w-5 text-yellow-400" aria-hidden="true" />
                  <div className="ml-3">
                    <h3 className="text-sm font-medium text-yellow-800">Attention</h3>
                    <p className="text-sm text-yellow-700 mt-1">
                      You are authorizing a refund of {refundData && formatCurrency(refundData.amount, transaction.currency)}.
                      This action will be logged and cannot be undone.
                    </p>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="mt-6 flex justify-end space-x-4">
              <button
                type="button"
                className="px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                onClick={() => setIsSecurityDialogOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                onClick={handleSecurityVerification}
                disabled={!securityPassword}
              >
                Verify & Process Refund
              </button>
            </div>
          </Dialog.Panel>
        </div>
      </Dialog>
    </div>
  );
};

export default PaymentRefundPage;