import React from 'react';
import PaymentStatusIndicator, { PaymentStatus } from './PaymentStatusIndicator';

/**
 * Payment type enumeration matching backend PaymentType
 */
export type PaymentType = 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';

/**
 * Interface representing a payment transaction
 */
export interface PaymentTransaction {
  /**
   * Unique identifier for the transaction
   */
  transactionId: string;
  
  /**
   * Transaction amount with currency
   */
  amount: number;
  
  /**
   * ISO 4217 currency code (3 characters)
   */
  currency: string;
  
  /**
   * Current status of the transaction
   */
  status: PaymentStatus;
  
  /**
   * Timestamp when the transaction was created
   */
  createdAt: string;
  
  /**
   * External merchant identifier
   */
  merchantId: string;
  
  /**
   * Merchant name for display
   */
  merchantName?: string;
  
  /**
   * Type of payment method used
   */
  paymentType: PaymentType;
  
  /**
   * External reference number
   */
  transactionReference?: string;
  
  /**
   * Human-readable description
   */
  description?: string;
}

/**
 * Props for the PaymentCard component
 */
interface PaymentCardProps {
  /**
   * The payment transaction to display
   */
  transaction: PaymentTransaction;
  
  /**
   * Optional click handler for the card
   */
  onClick?: (transactionId: string) => void;
  
  /**
   * Optional CSS class name to apply to the container
   */
  className?: string;
  
  /**
   * Whether to show the "View Details" button
   * @default true
   */
  showViewDetails?: boolean;
}

/**
 * PaymentCard component
 * 
 * A reusable card component for displaying payment transaction information in a consistent format.
 * It provides a standardized way to show payment details like transaction ID, amount, date, and status
 * with appropriate visual indicators.
 */
const PaymentCard: React.FC<PaymentCardProps> = ({
  transaction,
  onClick,
  className = '',
  showViewDetails = true
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
      day: 'numeric'
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
  
  // Handle card click
  const handleCardClick = () => {
    if (onClick) {
      onClick(transaction.transactionId);
    }
  };
  
  // Generate a shortened transaction ID for display
  const shortTransactionId = transaction.transactionId.substring(0, 8);
  
  return (
    <div 
      className={`bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden transition-all hover:shadow-md ${className} ${onClick ? 'cursor-pointer' : ''}`}
      onClick={onClick ? handleCardClick : undefined}
      data-testid="payment-card"
    >
      <div className="p-4">
        {/* Header with Transaction ID and Status */}
        <div className="flex justify-between items-start mb-3">
          <div>
            <h3 className="text-sm font-medium text-gray-900 dark:text-gray-100">
              Transaction #{shortTransactionId}
            </h3>
            {transaction.transactionReference && (
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Ref: {transaction.transactionReference}
              </p>
            )}
          </div>
          <PaymentStatusIndicator status={transaction.status} size="sm" />
        </div>
        
        {/* Main content with amount and date */}
        <div className="mb-3">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-gray-500 dark:text-gray-400">Amount</span>
            <span className="text-base font-semibold text-gray-900 dark:text-gray-100">
              {formatAmount(transaction.amount, transaction.currency)}
            </span>
          </div>
          
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-gray-500 dark:text-gray-400">Date</span>
            <span className="text-sm text-gray-700 dark:text-gray-300">
              {formatDate(transaction.createdAt)}
            </span>
          </div>
          
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-500 dark:text-gray-400">Method</span>
            <span className="text-sm text-gray-700 dark:text-gray-300">
              {getPaymentMethodDisplay(transaction.paymentType)}
            </span>
          </div>
          
          {transaction.merchantName && (
            <div className="flex justify-between items-center mt-2">
              <span className="text-sm text-gray-500 dark:text-gray-400">Merchant</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                {transaction.merchantName}
              </span>
            </div>
          )}
        </div>
        
        {/* Description if available */}
        {transaction.description && (
          <div className="mb-3 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
            {transaction.description}
          </div>
        )}
        
        {/* View Details button */}
        {showViewDetails && (
          <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-700">
            <button
              onClick={(e) => {
                e.stopPropagation();
                if (onClick) onClick(transaction.transactionId);
              }}
              className="w-full text-center text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 transition-colors"
              aria-label={`View details for transaction ${shortTransactionId}`}
            >
              View Details
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default PaymentCard;