import React from 'react';
import PaymentStatusIndicator, { PaymentStatus } from './PaymentStatusIndicator';
import { PaymentTransaction, PaymentType } from './PaymentCard';

/**
 * Interface representing payment method details
 */
interface PaymentMethodDetails {
  /**
   * Payment method identifier
   */
  paymentMethodId: string;
  
  /**
   * Masked payment token (e.g., last 4 digits of card)
   */
  paymentToken?: string;
  
  /**
   * Expiration date if applicable
   */
  expiration?: string;
  
  /**
   * Additional payment details as needed
   */
  details?: Record<string, any>;
  
  /**
   * Billing information
   */
  billingData?: Record<string, any>;
}

/**
 * Interface representing a payment event in the transaction timeline
 */
interface PaymentEvent {
  /**
   * Unique identifier for the event
   */
  eventId: string;
  
  /**
   * Type of event that occurred
   */
  eventType: string;
  
  /**
   * Status before the event if applicable
   */
  previousStatus?: string;
  
  /**
   * Status after the event if applicable
   */
  newStatus?: string;
  
  /**
   * Timestamp when the event occurred
   */
  createdAt: string;
  
  /**
   * User or system that created the event
   */
  createdBy: string;
  
  /**
   * Additional event data
   */
  eventData?: Record<string, any>;
}

/**
 * Props for the PaymentDetailPanel component
 */
interface PaymentDetailPanelProps {
  /**
   * The payment transaction to display
   */
  transaction: PaymentTransaction;
  
  /**
   * Payment method details associated with the transaction
   */
  paymentMethod?: PaymentMethodDetails;
  
  /**
   * Timeline of events for this transaction
   */
  events?: PaymentEvent[];
  
  /**
   * Callback for capture action
   */
  onCapture?: (transactionId: string) => void;
  
  /**
   * Callback for refund action
   */
  onRefund?: (transactionId: string) => void;
  
  /**
   * Callback for void action
   */
  onVoid?: (transactionId: string) => void;
  
  /**
   * Optional CSS class name to apply to the container
   */
  className?: string;
}

/**
 * PaymentDetailPanel component
 * 
 * A component that displays comprehensive payment transaction details in a structured layout.
 * It organizes payment information into logical sections including transaction details,
 * payment method information, and transaction history.
 */
const PaymentDetailPanel: React.FC<PaymentDetailPanelProps> = ({
  transaction,
  paymentMethod,
  events = [],
  onCapture,
  onRefund,
  onVoid,
  className = ''
}) => {
  // Format currency amount with proper decimal places and currency symbol
  const formatAmount = (amount: number, currency: string): string => {
    const formatter = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
    
    return formatter.format(amount);
  };
  
  // Format date to a readable format
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };
  
  // Get payment method display name
  const getPaymentMethodDisplay = (paymentType: PaymentType): string => {
    switch (paymentType) {
      case 'CREDIT_CARD':
        return 'Credit Card';
      case 'DEBIT_CARD':
        return 'Debit Card';
      case 'BANK_TRANSFER':
        return 'Bank Transfer';
      case 'DIGITAL_WALLET':
        return 'Digital Wallet';
      default:
        return paymentType;
    }
  };
  
  // Get event type display name
  const getEventTypeDisplay = (eventType: string): string => {
    switch (eventType) {
      case 'TRANSACTION_CREATED':
        return 'Transaction Created';
      case 'STATUS_CHANGE':
        return 'Status Changed';
      case 'PROCESSING_INITIATED':
        return 'Processing Initiated';
      case 'CAPTURE_INITIATED':
        return 'Capture Initiated';
      case 'REFUND_INITIATED':
        return 'Refund Initiated';
      case 'ERROR':
        return 'Error Occurred';
      default:
        return eventType.split('_').map(word => 
          word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        ).join(' ');
    }
  };
  
  // Get event icon based on event type
  const getEventIcon = (eventType: string): React.ReactNode => {
    switch (eventType) {
      case 'TRANSACTION_CREATED':
        return (
          <div className="h-8 w-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path d="M5.433 13.917l1.262-3.155A4 4 0 017.58 9.42l6.92-6.918a2.121 2.121 0 013 3l-6.92 6.918c-.383.383-.84.685-1.343.886l-3.154 1.262a.5.5 0 01-.65-.65z" />
              <path d="M3.5 5.75c0-.69.56-1.25 1.25-1.25H10A.75.75 0 0010 3H4.75A2.75 2.75 0 002 5.75v9.5A2.75 2.75 0 004.75 18h9.5A2.75 2.75 0 0017 15.25V10a.75.75 0 00-1.5 0v5.25c0 .69-.56 1.25-1.25 1.25h-9.5c-.69 0-1.25-.56-1.25-1.25v-9.5z" />
            </svg>
          </div>
        );
      case 'STATUS_CHANGE':
        return (
          <div className="h-8 w-8 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center text-purple-600 dark:text-purple-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path fillRule="evenodd" d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H3.989a.75.75 0 00-.75.75v4.242a.75.75 0 001.5 0v-2.43l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.39zm1.23-3.723a.75.75 0 00.219-.53V2.929a.75.75 0 00-1.5 0V5.36l-.31-.31A7 7 0 003.239 8.188a.75.75 0 101.448.389A5.5 5.5 0 0113.89 6.11l.311.31h-2.432a.75.75 0 000 1.5h4.243a.75.75 0 00.53-.219z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'PROCESSING_INITIATED':
        return (
          <div className="h-8 w-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5 animate-spin">
              <path fillRule="evenodd" d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H3.989a.75.75 0 00-.75.75v4.242a.75.75 0 001.5 0v-2.43l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.39zm1.23-3.723a.75.75 0 00.219-.53V2.929a.75.75 0 00-1.5 0V5.36l-.31-.31A7 7 0 003.239 8.188a.75.75 0 101.448.389A5.5 5.5 0 0113.89 6.11l.311.31h-2.432a.75.75 0 000 1.5h4.243a.75.75 0 00.53-.219z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'CAPTURE_INITIATED':
        return (
          <div className="h-8 w-8 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path fillRule="evenodd" d="M5 10a.75.75 0 01.75-.75h6.638L10.23 7.29a.75.75 0 111.04-1.08l3.5 3.25a.75.75 0 010 1.08l-3.5 3.25a.75.75 0 11-1.04-1.08l2.158-1.96H5.75A.75.75 0 015 10z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'REFUND_INITIATED':
        return (
          <div className="h-8 w-8 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center text-amber-600 dark:text-amber-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path fillRule="evenodd" d="M15 10a.75.75 0 01-.75.75H7.612l2.158 1.96a.75.75 0 11-1.04 1.08l-3.5-3.25a.75.75 0 010-1.08l3.5-3.25a.75.75 0 111.04 1.08L7.612 9.25h6.638A.75.75 0 0115 10z" clipRule="evenodd" />
            </svg>
          </div>
        );
      case 'ERROR':
        return (
          <div className="h-8 w-8 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center text-red-600 dark:text-red-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clipRule="evenodd" />
            </svg>
          </div>
        );
      default:
        return (
          <div className="h-8 w-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-gray-600 dark:text-gray-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z" clipRule="evenodd" />
            </svg>
          </div>
        );
    }
  };
  
  // Sort events by creation date (newest first)
  const sortedEvents = [...events].sort((a, b) => 
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
  
  // Determine if actions are available based on transaction status
  const canCapture = transaction.status === 'AUTHORIZED' && onCapture;
  const canRefund = transaction.status === 'CAPTURED' && onRefund;
  const canVoid = transaction.status === 'AUTHORIZED' && onVoid;
  
  return (
    <div 
      className={`bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden ${className}`}
      data-testid="payment-detail-panel"
    >
      {/* Header with Transaction ID and Status */}
      <div className="border-b border-gray-200 dark:border-gray-700 p-4 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
              Transaction Details
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              ID: {transaction.transactionId}
            </p>
            {transaction.transactionReference && (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Reference: {transaction.transactionReference}
              </p>
            )}
          </div>
          <PaymentStatusIndicator status={transaction.status} size="lg" />
        </div>
        
        {/* Action Buttons */}
        {(canCapture || canRefund || canVoid) && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
            {canCapture && (
              <button
                onClick={() => onCapture?.(transaction.transactionId)}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
                aria-label="Capture payment"
              >
                Capture Payment
              </button>
            )}
            {canRefund && (
              <button
                onClick={() => onRefund?.(transaction.transactionId)}
                className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
                aria-label="Refund payment"
              >
                Refund Payment
              </button>
            )}
            {canVoid && (
              <button
                onClick={() => onVoid?.(transaction.transactionId)}
                className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
                aria-label="Void payment"
              >
                Void Payment
              </button>
            )}
          </div>
        )}
      </div>
      
      {/* Main content with transaction details */}
      <div className="p-4 sm:p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Transaction Information Card */}
          <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4">
            <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              Transaction Information
            </h3>
            
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-sm text-gray-500 dark:text-gray-400">Amount</span>
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {formatAmount(transaction.amount, transaction.currency)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-sm text-gray-500 dark:text-gray-400">Date</span>
                <span className="text-sm text-gray-900 dark:text-gray-100">
                  {formatDate(transaction.createdAt)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-sm text-gray-500 dark:text-gray-400">Status</span>
                <span className="text-sm text-gray-900 dark:text-gray-100">
                  <PaymentStatusIndicator status={transaction.status} size="sm" />
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-sm text-gray-500 dark:text-gray-400">Payment Method</span>
                <span className="text-sm text-gray-900 dark:text-gray-100">
                  {getPaymentMethodDisplay(transaction.paymentType)}
                </span>
              </div>
              
              <div className="flex justify-between">
                <span className="text-sm text-gray-500 dark:text-gray-400">Merchant</span>
                <span className="text-sm text-gray-900 dark:text-gray-100">
                  {transaction.merchantName || transaction.merchantId}
                </span>
              </div>
              
              {transaction.description && (
                <div className="pt-2 border-t border-gray-200 dark:border-gray-700">
                  <span className="text-sm text-gray-500 dark:text-gray-400 block mb-1">Description</span>
                  <p className="text-sm text-gray-900 dark:text-gray-100">
                    {transaction.description}
                  </p>
                </div>
              )}
            </div>
          </div>
          
          {/* Payment Method Card */}
          <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4">
            <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
              Payment Method Details
            </h3>
            
            {paymentMethod ? (
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-500 dark:text-gray-400">Method Type</span>
                  <span className="text-sm text-gray-900 dark:text-gray-100">
                    {getPaymentMethodDisplay(transaction.paymentType)}
                  </span>
                </div>
                
                {paymentMethod.paymentToken && (
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-500 dark:text-gray-400">
                      {transaction.paymentType.includes('CARD') ? 'Card Number' : 'Token'}
                    </span>
                    <span className="text-sm font-mono text-gray-900 dark:text-gray-100">
                      {paymentMethod.paymentToken}
                    </span>
                  </div>
                )}
                
                {paymentMethod.expiration && (
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-500 dark:text-gray-400">Expiration</span>
                    <span className="text-sm text-gray-900 dark:text-gray-100">
                      {paymentMethod.expiration}
                    </span>
                  </div>
                )}
                
                <div className="flex justify-between">
                  <span className="text-sm text-gray-500 dark:text-gray-400">Payment ID</span>
                  <span className="text-sm font-mono text-gray-900 dark:text-gray-100">
                    {paymentMethod.paymentMethodId}
                  </span>
                </div>
                
                {paymentMethod.billingData && (
                  <div className="pt-2 border-t border-gray-200 dark:border-gray-700">
                    <span className="text-sm text-gray-500 dark:text-gray-400 block mb-1">Billing Information</span>
                    {paymentMethod.billingData.name && (
                      <p className="text-sm text-gray-900 dark:text-gray-100">
                        {paymentMethod.billingData.name}
                      </p>
                    )}
                    {paymentMethod.billingData.address && (
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        {paymentMethod.billingData.address}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                No detailed payment method information available.
              </p>
            )}
          </div>
        </div>
        
        {/* Transaction Timeline */}
        <div className="mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">
            Transaction Timeline
          </h3>
          
          {sortedEvents.length > 0 ? (
            <div className="flow-root">
              <ul className="-mb-8">
                {sortedEvents.map((event, eventIdx) => (
                  <li key={event.eventId}>
                    <div className="relative pb-8">
                      {eventIdx !== sortedEvents.length - 1 ? (
                        <span
                          className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200 dark:bg-gray-700"
                          aria-hidden="true"
                        />
                      ) : null}
                      <div className="relative flex space-x-3">
                        <div>
                          {getEventIcon(event.eventType)}
                        </div>
                        <div className="flex min-w-0 flex-1 justify-between space-x-4 pt-1.5">
                          <div>
                            <p className="text-sm text-gray-900 dark:text-gray-100">
                              {getEventTypeDisplay(event.eventType)}
                              {event.previousStatus && event.newStatus && (
                                <span className="text-gray-500 dark:text-gray-400">
                                  : {event.previousStatus} → {event.newStatus}
                                </span>
                              )}
                            </p>
                            {event.eventData && (
                              <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                                {typeof event.eventData === 'object' 
                                  ? Object.entries(event.eventData)
                                      .map(([key, value]) => `${key}: ${value}`)
                                      .join(', ')
                                  : event.eventData
                                }
                              </p>
                            )}
                          </div>
                          <div className="whitespace-nowrap text-right text-sm text-gray-500 dark:text-gray-400">
                            <time dateTime={event.createdAt}>{formatDate(event.createdAt)}</time>
                            <div className="text-xs">{event.createdBy}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No event history available for this transaction.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default PaymentDetailPanel;