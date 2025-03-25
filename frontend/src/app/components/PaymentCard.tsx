import React from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';

// Define payment status types
export type PaymentStatus = 'CREATED' | 'PROCESSING' | 'AUTHORIZED' | 'CAPTURED' | 'REFUNDED' | 'FAILED' | 'VOIDED';

// Define payment type
export type PaymentType = 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';

// Define payment transaction interface
export interface PaymentTransaction {
  transactionId: string;
  organizationId: string;
  accountId: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
  merchantId: string;
  paymentType: PaymentType;
  transactionReference?: string;
  description?: string;
}

// Props for the PaymentCard component
interface PaymentCardProps {
  transaction: PaymentTransaction;
  showDetails?: boolean;
}

/**
 * PaymentCard component displays payment transaction information in a card format
 * 
 * This component provides a standardized way to show payment details like
 * transaction ID, amount, date, and status with appropriate visual indicators.
 * It adapts responsively between mobile stacked view and desktop tabular view.
 */
const PaymentCard: React.FC<PaymentCardProps> = ({ 
  transaction, 
  showDetails = false 
}) => {
  // Format date to readable string
  const formattedDate = format(new Date(transaction.createdAt), 'MMM dd, yyyy');
  
  // Format currency with proper symbol and decimal places
  const formatCurrency = (amount: number, currency: string): string => {
    const formatter = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    
    return formatter.format(amount);
  };

  // Get status indicator color based on payment status
  const getStatusColor = (status: PaymentStatus): string => {
    switch (status) {
      case 'CAPTURED':
      case 'AUTHORIZED':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'PROCESSING':
      case 'CREATED':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'FAILED':
      case 'VOIDED':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      case 'REFUNDED':
        return 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
    }
  };

  // Get status icon based on payment status
  const getStatusIcon = (status: PaymentStatus): string => {
    switch (status) {
      case 'CAPTURED':
      case 'AUTHORIZED':
        return '✓';
      case 'PROCESSING':
      case 'CREATED':
        return '⟳';
      case 'FAILED':
      case 'VOIDED':
        return '✕';
      case 'REFUNDED':
        return '↺';
      default:
        return '?';
    }
  };

  // Get payment type display name
  const getPaymentTypeDisplay = (type: PaymentType): string => {
    switch (type) {
      case 'CREDIT_CARD':
        return 'Credit Card';
      case 'DEBIT_CARD':
        return 'Debit Card';
      case 'BANK_TRANSFER':
        return 'Bank Transfer';
      case 'DIGITAL_WALLET':
        return 'Digital Wallet';
      default:
        return type;
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden transition-all hover:shadow-lg">
      {/* Card Header */}
      <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
        <div className="font-medium text-gray-700 dark:text-gray-300">
          Transaction #{transaction.transactionId.substring(0, 8)}
        </div>
        <div className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(transaction.status)}`}>
          <span className="mr-1">{getStatusIcon(transaction.status)}</span>
          {transaction.status}
        </div>
      </div>
      
      {/* Card Body */}
      <div className="p-4">
        <div className="grid grid-cols-2 gap-4">
          {/* Amount */}
          <div className="col-span-2 sm:col-span-1">
            <div className="text-sm text-gray-500 dark:text-gray-400">Amount</div>
            <div className="text-lg font-semibold text-gray-900 dark:text-white">
              {formatCurrency(transaction.amount, transaction.currency)}
            </div>
          </div>
          
          {/* Date */}
          <div className="col-span-2 sm:col-span-1">
            <div className="text-sm text-gray-500 dark:text-gray-400">Date</div>
            <div className="text-gray-900 dark:text-white">{formattedDate}</div>
          </div>
          
          {/* Merchant */}
          <div className="col-span-2 sm:col-span-1">
            <div className="text-sm text-gray-500 dark:text-gray-400">Merchant</div>
            <div className="text-gray-900 dark:text-white">{transaction.merchantId}</div>
          </div>
          
          {/* Payment Method */}
          <div className="col-span-2 sm:col-span-1">
            <div className="text-sm text-gray-500 dark:text-gray-400">Payment Method</div>
            <div className="text-gray-900 dark:text-white">
              {getPaymentTypeDisplay(transaction.paymentType)}
            </div>
          </div>
          
          {/* Conditional rendering for additional details */}
          {showDetails && transaction.description && (
            <div className="col-span-2">
              <div className="text-sm text-gray-500 dark:text-gray-400">Description</div>
              <div className="text-gray-900 dark:text-white">{transaction.description}</div>
            </div>
          )}
          
          {showDetails && transaction.transactionReference && (
            <div className="col-span-2">
              <div className="text-sm text-gray-500 dark:text-gray-400">Reference</div>
              <div className="text-gray-900 dark:text-white">{transaction.transactionReference}</div>
            </div>
          )}
        </div>
      </div>
      
      {/* Card Footer */}
      <div className="px-4 py-3 bg-gray-50 dark:bg-gray-700 border-t border-gray-200 dark:border-gray-600">
        <Link 
          to={`/payments/${transaction.organizationId}/accounts/${transaction.accountId}/transactions/${transaction.transactionId}`}
          className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 text-sm font-medium flex items-center"
        >
          View Details
          <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </Link>
      </div>
    </div>
  );
};

export default PaymentCard;