import React from 'react';

// Define payment status types - matching the types from PaymentCard
export type PaymentStatus = 'CREATED' | 'PROCESSING' | 'AUTHORIZED' | 'CAPTURED' | 'REFUNDED' | 'FAILED' | 'VOIDED' | 'PENDING';

// Props for the PaymentStatusIndicator component
interface PaymentStatusIndicatorProps {
  /**
   * The current status of the payment transaction
   */
  status: PaymentStatus;
  
  /**
   * Whether to show the text label alongside the icon
   * @default true
   */
  showLabel?: boolean;
  
  /**
   * Optional CSS class name to apply to the container
   */
  className?: string;
  
  /**
   * Size of the indicator
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';
}

/**
 * PaymentStatusIndicator component
 * 
 * A component that visually represents payment transaction status through 
 * color-coded icons and labels. It ensures consistent status representation
 * across the application by mapping status codes to appropriate visual indicators.
 */
const PaymentStatusIndicator: React.FC<PaymentStatusIndicatorProps> = ({
  status,
  showLabel = true,
  className = '',
  size = 'md'
}) => {
  // Get status color based on payment status
  const getStatusColor = (status: PaymentStatus): string => {
    switch (status) {
      case 'CAPTURED':
      case 'AUTHORIZED':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'PROCESSING':
      case 'CREATED':
      case 'PENDING':
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
      case 'PENDING':
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

  // Get human-readable status label
  const getStatusLabel = (status: PaymentStatus): string => {
    switch (status) {
      case 'CAPTURED':
        return 'Captured';
      case 'AUTHORIZED':
        return 'Authorized';
      case 'PROCESSING':
        return 'Processing';
      case 'CREATED':
        return 'Created';
      case 'PENDING':
        return 'Pending';
      case 'FAILED':
        return 'Failed';
      case 'VOIDED':
        return 'Voided';
      case 'REFUNDED':
        return 'Refunded';
      default:
        return status;
    }
  };

  // Get ARIA label for accessibility
  const getAriaLabel = (status: PaymentStatus): string => {
    switch (status) {
      case 'CAPTURED':
        return 'Payment successfully captured';
      case 'AUTHORIZED':
        return 'Payment authorized but not captured';
      case 'PROCESSING':
        return 'Payment is being processed';
      case 'CREATED':
        return 'Payment created but not processed';
      case 'PENDING':
        return 'Payment pending completion';
      case 'FAILED':
        return 'Payment failed';
      case 'VOIDED':
        return 'Payment voided';
      case 'REFUNDED':
        return 'Payment refunded';
      default:
        return `Payment status: ${status}`;
    }
  };

  // Determine size classes
  const getSizeClasses = (size: 'sm' | 'md' | 'lg'): string => {
    switch (size) {
      case 'sm':
        return 'text-xs px-1.5 py-0.5';
      case 'lg':
        return 'text-base px-3 py-1.5';
      case 'md':
      default:
        return 'text-xs px-2 py-1';
    }
  };

  const sizeClasses = getSizeClasses(size);
  const statusColor = getStatusColor(status);
  const statusIcon = getStatusIcon(status);
  const statusLabel = getStatusLabel(status);
  const ariaLabel = getAriaLabel(status);

  return (
    <div 
      className={`inline-flex items-center rounded-full font-medium ${statusColor} ${sizeClasses} ${className}`}
      aria-label={ariaLabel}
      role="status"
    >
      <span className="mr-1" aria-hidden="true">{statusIcon}</span>
      {showLabel && <span>{statusLabel}</span>}
    </div>
  );
};

export default PaymentStatusIndicator;