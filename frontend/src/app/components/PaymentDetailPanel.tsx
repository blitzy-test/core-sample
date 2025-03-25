import React from 'react';
import PaymentStatusIndicator, { PaymentStatus } from './PaymentStatusIndicator';
import PaymentAmountDisplay from './PaymentAmountDisplay';
import PaymentActionButton, { PaymentActionType, PaymentStatusType } from './PaymentActionButton';

/**
 * Interface for a payment event in the timeline
 */
interface PaymentEvent {
  eventId: string;
  eventType: string;
  timestamp: string;
  previousStatus?: string;
  newStatus?: string;
  createdBy: string;
  eventData?: Record<string, any>;
}

/**
 * Interface for payment method details
 */
interface PaymentMethodDetails {
  paymentMethodId: string;
  paymentType: string;
  cardType?: string;
  cardNumber?: string;
  expirationDate?: string;
  cardholderName?: string;
  billingAddress?: {
    line1?: string;
    line2?: string;
    city?: string;
    state?: string;
    postalCode?: string;
    country?: string;
  };
}

/**
 * Interface for payment transaction details
 */
interface PaymentTransaction {
  transactionId: string;
  organizationId: string;
  accountId: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
  merchantId: string;
  merchantName: string;
  paymentType: string;
  transactionReference?: string;
  description?: string;
  paymentMethod?: PaymentMethodDetails;
  events?: PaymentEvent[];
  fees?: Array<{
    feeId: string;
    feeType: string;
    amount: number;
    currency: string;
    description?: string;
  }>;
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
   * Optional CSS class name for the container
   */
  className?: string;
  
  /**
   * Optional user permissions for action buttons
   */
  userPermissions?: string[];
  
  /**
   * Optional callback when an action is completed
   */
  onActionComplete?: () => void;
  
  /**
   * Optional flag to show/hide action buttons
   */
  showActions?: boolean;
}

/**
 * A component that displays comprehensive payment transaction details in a structured layout.
 * It organizes payment information into logical sections including transaction details,
 * payment method information, and transaction history.
 */
const PaymentDetailPanel: React.FC<PaymentDetailPanelProps> = ({
  transaction,
  className = '',
  userPermissions = [],
  onActionComplete,
  showActions = true
}) => {
  // Format date for display
  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        hour12: true
      }).format(date);
    } catch (error) {
      console.error('Error formatting date:', error);
      return dateString;
    }
  };
  
  // Map PaymentStatus to PaymentStatusType for action buttons
  const mapStatusForActions = (status: PaymentStatus): PaymentStatusType => {
    const statusMap: Record<PaymentStatus, PaymentStatusType> = {
      'CREATED': 'pending',
      'PROCESSING': 'processing',
      'AUTHORIZED': 'authorized',
      'CAPTURED': 'captured',
      'REFUNDED': 'refunded',
      'FAILED': 'failed',
      'VOIDED': 'voided',
      'PENDING': 'pending'
    };
    
    return statusMap[status] || 'pending';
  };
  
  // Determine available actions based on transaction status
  const getAvailableActions = (status: PaymentStatus): PaymentActionType[] => {
    switch (status) {
      case 'CREATED':
      case 'PENDING':
        return ['authorize', 'void'];
      case 'AUTHORIZED':
        return ['capture', 'void'];
      case 'CAPTURED':
        return ['refund'];
      case 'PROCESSING':
        return [];
      case 'REFUNDED':
      case 'FAILED':
      case 'VOIDED':
        return [];
      default:
        return [];
    }
  };
  
  // Get masked card number for display
  const getMaskedCardNumber = (cardNumber?: string): string => {
    if (!cardNumber) return 'N/A';
    
    // Keep only the last 4 digits visible
    if (cardNumber.length > 4) {
      const lastFour = cardNumber.slice(-4);
      const maskedPart = 'XXXX-XXXX-XXXX-';
      return maskedPart + lastFour;
    }
    
    return cardNumber;
  };
  
  // Get next available actions text based on status
  const getNextActionsText = (status: PaymentStatus): string => {
    switch (status) {
      case 'CREATED':
      case 'PENDING':
        return 'Authorize the transaction or void it if no longer needed.';
      case 'AUTHORIZED':
        return 'Capture funds to complete the transaction or void the authorization.';
      case 'CAPTURED':
        return 'Issue a refund if needed.';
      case 'PROCESSING':
        return 'Wait for processing to complete.';
      case 'REFUNDED':
        return 'Transaction has been refunded. No further actions available.';
      case 'FAILED':
        return 'Transaction failed. Create a new transaction if needed.';
      case 'VOIDED':
        return 'Transaction has been voided. No further actions available.';
      default:
        return 'No actions available for the current status.';
    }
  };
  
  return (
    <div className={`bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden ${className}`}>
      {/* Transaction Header */}
      <div className="p-6 border-b border-gray-200 dark:border-gray-700">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
              Payment Transaction
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              ID: {transaction.transactionId}
            </p>
          </div>
          <div className="mt-4 sm:mt-0">
            <PaymentStatusIndicator 
              status={transaction.status} 
              size="lg" 
              className="font-semibold"
            />
          </div>
        </div>
      </div>
      
      {/* Main Content - Two Column Layout on Desktop */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 p-6">
        {/* Transaction Details - Left Column (2/3 width on desktop) */}
        <div className="md:col-span-2 space-y-6">
          {/* Transaction Details Card */}
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
              Transaction Details
            </h3>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Amount</p>
                <p className="mt-1 text-base font-semibold">
                  <PaymentAmountDisplay 
                    amount={transaction.amount} 
                    currency={transaction.currency}
                  />
                </p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Currency</p>
                <p className="mt-1 text-base">{transaction.currency}</p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Date Created</p>
                <p className="mt-1 text-base">{formatDate(transaction.createdAt)}</p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Last Updated</p>
                <p className="mt-1 text-base">{formatDate(transaction.updatedAt)}</p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Merchant</p>
                <p className="mt-1 text-base">{transaction.merchantName}</p>
              </div>
              
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Payment Type</p>
                <p className="mt-1 text-base">{transaction.paymentType}</p>
              </div>
              
              {transaction.transactionReference && (
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Reference</p>
                  <p className="mt-1 text-base">{transaction.transactionReference}</p>
                </div>
              )}
              
              {transaction.description && (
                <div className="sm:col-span-2">
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Description</p>
                  <p className="mt-1 text-base">{transaction.description}</p>
                </div>
              )}
            </div>
          </div>
          
          {/* Payment Method Details Card */}
          {transaction.paymentMethod && (
            <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Payment Method
              </h3>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Method Type</p>
                  <p className="mt-1 text-base">{transaction.paymentMethod.paymentType}</p>
                </div>
                
                {transaction.paymentMethod.cardType && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Card Type</p>
                    <p className="mt-1 text-base">{transaction.paymentMethod.cardType}</p>
                  </div>
                )}
                
                {transaction.paymentMethod.cardNumber && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Card Number</p>
                    <p className="mt-1 text-base font-mono">{getMaskedCardNumber(transaction.paymentMethod.cardNumber)}</p>
                  </div>
                )}
                
                {transaction.paymentMethod.expirationDate && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Expiration Date</p>
                    <p className="mt-1 text-base">{transaction.paymentMethod.expirationDate}</p>
                  </div>
                )}
                
                {transaction.paymentMethod.cardholderName && (
                  <div>
                    <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Cardholder Name</p>
                    <p className="mt-1 text-base">{transaction.paymentMethod.cardholderName}</p>
                  </div>
                )}
              </div>
              
              {/* Billing Address if available */}
              {transaction.paymentMethod.billingAddress && (
                <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                  <h4 className="text-md font-medium text-gray-900 dark:text-white mb-2">
                    Billing Address
                  </h4>
                  
                  <address className="not-italic text-sm text-gray-600 dark:text-gray-300">
                    {transaction.paymentMethod.billingAddress.line1 && (
                      <p>{transaction.paymentMethod.billingAddress.line1}</p>
                    )}
                    {transaction.paymentMethod.billingAddress.line2 && (
                      <p>{transaction.paymentMethod.billingAddress.line2}</p>
                    )}
                    {transaction.paymentMethod.billingAddress.city && transaction.paymentMethod.billingAddress.state && (
                      <p>
                        {transaction.paymentMethod.billingAddress.city}, {transaction.paymentMethod.billingAddress.state} {transaction.paymentMethod.billingAddress.postalCode}
                      </p>
                    )}
                    {transaction.paymentMethod.billingAddress.country && (
                      <p>{transaction.paymentMethod.billingAddress.country}</p>
                    )}
                  </address>
                </div>
              )}
            </div>
          )}
          
          {/* Fees Section if available */}
          {transaction.fees && transaction.fees.length > 0 && (
            <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Transaction Fees
              </h3>
              
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-100 dark:bg-gray-800">
                    <tr>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        Fee Type
                      </th>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        Amount
                      </th>
                      <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        Description
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                    {transaction.fees.map((fee) => (
                      <tr key={fee.feeId}>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                          {fee.feeType}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm">
                          <PaymentAmountDisplay 
                            amount={fee.amount} 
                            currency={fee.currency}
                          />
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                          {fee.description || '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
        
        {/* Right Column (1/3 width on desktop) */}
        <div className="space-y-6">
          {/* Actions Card */}
          {showActions && (
            <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Actions
              </h3>
              
              <div className="flex flex-col space-y-3">
                {getAvailableActions(transaction.status).map((actionType) => (
                  <PaymentActionButton
                    key={actionType}
                    actionType={actionType}
                    paymentStatus={mapStatusForActions(transaction.status)}
                    transactionId={transaction.transactionId}
                    organizationId={transaction.organizationId}
                    accountId={transaction.accountId}
                    amount={transaction.amount.toString()}
                    currency={transaction.currency}
                    onActionComplete={onActionComplete}
                    userPermissions={userPermissions}
                    className="w-full justify-center"
                  />
                ))}
                
                {getAvailableActions(transaction.status).length === 0 && (
                  <p className="text-sm text-gray-500 dark:text-gray-400 italic">
                    No actions available for the current transaction status.
                  </p>
                )}
              </div>
              
              <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                <h4 className="text-sm font-medium text-gray-900 dark:text-white mb-2">
                  Next Steps
                </h4>
                <p className="text-sm text-gray-600 dark:text-gray-300">
                  {getNextActionsText(transaction.status)}
                </p>
              </div>
            </div>
          )}
          
          {/* Download Options */}
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-6">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
              Download
            </h3>
            
            <div className="flex flex-col space-y-3">
              <button
                type="button"
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
                </svg>
                Download Receipt
              </button>
              
              <button
                type="button"
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Export as PDF
              </button>
              
              <button
                type="button"
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Export as CSV
              </button>
            </div>
          </div>
        </div>
      </div>
      
      {/* Transaction Timeline Section */}
      {transaction.events && transaction.events.length > 0 && (
        <div className="border-t border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-6">
            Transaction Timeline
          </h3>
          
          <div className="flow-root">
            <ul className="-mb-8">
              {transaction.events.map((event, eventIdx) => (
                <li key={event.eventId}>
                  <div className="relative pb-8">
                    {eventIdx !== transaction.events!.length - 1 ? (
                      <span className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-gray-200 dark:bg-gray-700" aria-hidden="true" />
                    ) : null}
                    <div className="relative flex space-x-3">
                      <div>
                        <span className="h-8 w-8 rounded-full flex items-center justify-center ring-8 ring-white dark:ring-gray-800 bg-gray-100 dark:bg-gray-700">
                          {/* Icon based on event type */}
                          {event.eventType.includes('CREATE') && (
                            <svg className="h-5 w-5 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                            </svg>
                          )}
                          {event.eventType.includes('AUTHORIZE') && (
                            <svg className="h-5 w-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                            </svg>
                          )}
                          {event.eventType.includes('CAPTURE') && (
                            <svg className="h-5 w-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          )}
                          {event.eventType.includes('REFUND') && (
                            <svg className="h-5 w-5 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                            </svg>
                          )}
                          {event.eventType.includes('VOID') && (
                            <svg className="h-5 w-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          )}
                          {event.eventType.includes('FAIL') && (
                            <svg className="h-5 w-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          )}
                          {!event.eventType.includes('CREATE') && 
                            !event.eventType.includes('AUTHORIZE') && 
                            !event.eventType.includes('CAPTURE') && 
                            !event.eventType.includes('REFUND') && 
                            !event.eventType.includes('VOID') && 
                            !event.eventType.includes('FAIL') && (
                            <svg className="h-5 w-5 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          )}
                        </span>
                      </div>
                      <div className="min-w-0 flex-1 pt-1.5 flex justify-between space-x-4">
                        <div>
                          <p className="text-sm text-gray-900 dark:text-white">
                            {event.eventType.replace(/_/g, ' ')}
                            {event.previousStatus && event.newStatus && (
                              <span className="text-gray-500 dark:text-gray-400">
                                {' '}(Status changed from <span className="font-medium">{event.previousStatus}</span> to <span className="font-medium">{event.newStatus}</span>)
                              </span>
                            )}
                          </p>
                          {event.eventData && Object.keys(event.eventData).length > 0 && (
                            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                              {Object.entries(event.eventData).map(([key, value]) => (
                                <span key={key} className="block">
                                  <span className="font-medium">{key.replace(/_/g, ' ')}:</span> {value}
                                </span>
                              ))}
                            </p>
                          )}
                        </div>
                        <div className="text-right text-xs whitespace-nowrap text-gray-500 dark:text-gray-400">
                          <time dateTime={event.timestamp}>{formatDate(event.timestamp)}</time>
                          <p className="mt-0.5">{event.createdBy}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default PaymentDetailPanel;