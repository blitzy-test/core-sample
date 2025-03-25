import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { fetchTransactionDetails, fetchTransactionEvents } from '../../api/payments/transactionApi';
import { PaymentStatus, PaymentTransaction, PaymentEvent } from '../../api/payments/types';
import StatusBadge from '../../components/payments/StatusBadge';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorDisplay from '../../components/ui/ErrorDisplay';
import ConfirmationDialog from '../../components/modals/ConfirmationDialog';

/**
 * PaymentDetailPage displays comprehensive information about a single payment transaction
 * including transaction details, payment timeline history, and contextual action buttons
 * based on the current payment status.
 */
const PaymentDetailPage: React.FC = () => {
  const { orgId, accountId, transactionId } = useParams<{
    orgId: string;
    accountId: string;
    transactionId: string;
  }>();
  const navigate = useNavigate();
  const [showVoidConfirmation, setShowVoidConfirmation] = useState(false);

  // Fetch transaction details
  const {
    data: transaction,
    isLoading: isLoadingTransaction,
    isError: isTransactionError,
    error: transactionError
  } = useQuery({
    queryKey: ['transaction', orgId, accountId, transactionId],
    queryFn: () => fetchTransactionDetails(orgId!, accountId!, transactionId!),
    enabled: !!orgId && !!accountId && !!transactionId,
    staleTime: 30000 // 30 seconds
  });

  // Fetch transaction events/timeline
  const {
    data: events,
    isLoading: isLoadingEvents,
    isError: isEventsError,
    error: eventsError
  } = useQuery({
    queryKey: ['transactionEvents', orgId, accountId, transactionId],
    queryFn: () => fetchTransactionEvents(orgId!, accountId!, transactionId!),
    enabled: !!orgId && !!accountId && !!transactionId,
    staleTime: 30000 // 30 seconds
  });

  // Handle navigation to capture page
  const handleCapture = () => {
    navigate(`/organizations/${orgId}/accounts/${accountId}/transactions/${transactionId}/capture`);
  };

  // Handle navigation to refund page
  const handleRefund = () => {
    navigate(`/organizations/${orgId}/accounts/${accountId}/transactions/${transactionId}/refund`);
  };

  // Handle void confirmation dialog
  const handleVoidConfirmation = () => {
    setShowVoidConfirmation(true);
  };

  // Handle void transaction action
  const handleVoidTransaction = async () => {
    setShowVoidConfirmation(false);
    // Implementation would call the void API endpoint
    // For now, just navigate back to the transaction list
    navigate(`/organizations/${orgId}/accounts/${accountId}/transactions`);
  };

  // Format currency amount with proper symbol
  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(amount);
  };

  // Determine which action buttons to show based on transaction status
  const renderActionButtons = (transaction: PaymentTransaction) => {
    switch (transaction.status) {
      case PaymentStatus.AUTHORIZED:
        return (
          <>
            <button
              onClick={handleCapture}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Capture
            </button>
            <button
              onClick={handleVoidConfirmation}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 ml-2"
            >
              Void
            </button>
          </>
        );
      case PaymentStatus.CAPTURED:
        return (
          <button
            onClick={handleRefund}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            Refund
          </button>
        );
      default:
        return null;
    }
  };

  // Show loading state
  if (isLoadingTransaction || isLoadingEvents) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner />
      </div>
    );
  }

  // Show error state
  if (isTransactionError || isEventsError) {
    return (
      <ErrorDisplay 
        error={transactionError || eventsError} 
        message="Failed to load transaction details" 
      />
    );
  }

  // If data is loaded, render the transaction details
  return (
    <div className="container mx-auto px-4 py-6">
      {/* Back navigation */}
      <div className="mb-6">
        <Link
          to={`/organizations/${orgId}/accounts/${accountId}/transactions`}
          className="text-blue-600 hover:text-blue-800 flex items-center"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-5 w-5 mr-1"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M9.707 14.707a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 1.414L7.414 9H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 010 1.414z"
              clipRule="evenodd"
            />
          </svg>
          Back to Payments
        </Link>
      </div>

      {/* Transaction header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-2">
          Payment Transaction #{transaction?.transaction_id.substring(0, 8)}
        </h1>
      </div>

      {/* Transaction status card */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="flex justify-between items-center">
          <div className="flex items-center">
            <span className="font-semibold mr-2">Transaction Status:</span>
            <StatusBadge status={transaction?.status || PaymentStatus.UNKNOWN} />
          </div>
          <div>
            {transaction && renderActionButtons(transaction)}
          </div>
        </div>
      </div>

      {/* Main content grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Transaction details card */}
        <div className="md:col-span-2">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4">Transaction Details</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-gray-600 text-sm">Amount</p>
                <p className="font-semibold">
                  {transaction && formatCurrency(transaction.amount, transaction.currency)}
                </p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Currency</p>
                <p className="font-semibold">{transaction?.currency}</p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Date Created</p>
                <p className="font-semibold">
                  {transaction?.created_at && format(new Date(transaction.created_at), 'MM/dd/yyyy HH:mm:ss')}
                </p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Merchant</p>
                <p className="font-semibold">{transaction?.merchant_id}</p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Payment Type</p>
                <p className="font-semibold">{transaction?.payment_type}</p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Card Number</p>
                <p className="font-semibold">
                  {transaction?.payment_data?.card_number || 'XXXX-XXXX-XXXX-XXXX'}
                </p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Reference</p>
                <p className="font-semibold">{transaction?.transaction_reference || 'N/A'}</p>
              </div>
              <div>
                <p className="text-gray-600 text-sm">Last Updated</p>
                <p className="font-semibold">
                  {transaction?.updated_at && format(new Date(transaction.updated_at), 'MM/dd/yyyy HH:mm:ss')}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Actions card */}
        <div className="md:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4">Actions</h2>
            <div className="flex flex-col space-y-3">
              {transaction && renderActionButtons(transaction)}
              <button
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 flex items-center justify-center"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5 mr-2"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path
                    fillRule="evenodd"
                    d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
                    clipRule="evenodd"
                  />
                </svg>
                Download
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Transaction timeline */}
      <div className="mt-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">Transaction Timeline</h2>
          <div className="space-y-6">
            {events && events.length > 0 ? (
              events.map((event: PaymentEvent, index: number) => (
                <div key={event.event_id} className="relative pl-8">
                  {/* Timeline connector */}
                  {index < events.length - 1 && (
                    <div className="absolute left-3 top-6 bottom-0 w-0.5 bg-gray-300"></div>
                  )}
                  {/* Timeline dot */}
                  <div className="absolute left-0 top-1.5 w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center">
                    <span className="text-white text-xs">{index + 1}</span>
                  </div>
                  {/* Event content */}
                  <div>
                    <p className="font-semibold">
                      {format(new Date(event.created_at), 'MM/dd/yyyy HH:mm:ss')} - {event.event_type}
                    </p>
                    <p className="text-gray-600 text-sm mt-1">
                      {event.previous_status && (
                        <span>
                          Status changed from <StatusBadge status={event.previous_status} size="small" /> to{' '}
                          <StatusBadge status={event.new_status || ''} size="small" />
                        </span>
                      )}
                      {!event.previous_status && event.event_data && (
                        <span>{JSON.stringify(event.event_data)}</span>
                      )}
                    </p>
                    <p className="text-gray-500 text-xs mt-1">By: {event.created_by}</p>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-gray-500">No event history available</p>
            )}
          </div>

          {/* Next available actions */}
          {transaction && transaction.status === PaymentStatus.AUTHORIZED && (
            <div className="mt-6 pt-6 border-t border-gray-200">
              <h3 className="font-semibold mb-2">Next Available Actions:</h3>
              <ul className="list-disc list-inside text-gray-600">
                <li>Capture full or partial amount</li>
                <li>Void authorization</li>
              </ul>
            </div>
          )}
          
          {transaction && transaction.status === PaymentStatus.CAPTURED && (
            <div className="mt-6 pt-6 border-t border-gray-200">
              <h3 className="font-semibold mb-2">Next Available Actions:</h3>
              <ul className="list-disc list-inside text-gray-600">
                <li>Refund full or partial amount</li>
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* Void confirmation dialog */}
      {showVoidConfirmation && (
        <ConfirmationDialog
          title="Confirm Void Transaction"
          message={
            <>
              <p className="mb-4">Are you sure you want to void this transaction?</p>
              <p className="mb-2">
                <span className="font-semibold">Transaction ID:</span> {transaction?.transaction_id}
              </p>
              <p className="mb-2">
                <span className="font-semibold">Amount:</span>{' '}
                {transaction && formatCurrency(transaction.amount, transaction.currency)}
              </p>
              <p className="mb-4">
                <span className="font-semibold">Merchant:</span> {transaction?.merchant_id}
              </p>
              <p className="text-red-600 font-semibold">This action cannot be undone.</p>
            </>
          }
          confirmLabel="Void Transaction"
          cancelLabel="Cancel"
          onConfirm={handleVoidTransaction}
          onCancel={() => setShowVoidConfirmation(false)}
          isDestructive={true}
        />
      )}
    </div>
  );
};

export default PaymentDetailPage;