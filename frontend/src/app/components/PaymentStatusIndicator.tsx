import React from 'react';

/**
 * Payment status enumeration matching backend PaymentStatus
 */
export type PaymentStatus = 
  | 'CREATED' 
  | 'PROCESSING' 
  | 'AUTHORIZED' 
  | 'CAPTURED' 
  | 'REFUNDED' 
  | 'FAILED' 
  | 'VOIDED';

/**
 * Size options for the status indicator
 */
export type StatusSize = 'sm' | 'md' | 'lg';

/**
 * Props for the PaymentStatusIndicator component
 */
interface PaymentStatusIndicatorProps {
  /**
   * The payment status to display
   */
  status: PaymentStatus;
  
  /**
   * Whether to show the status text label alongside the icon
   * @default true
   */
  showLabel?: boolean;
  
  /**
   * Size of the status indicator
   * @default 'md'
   */
  size?: StatusSize;
  
  /**
   * Optional CSS class name to apply to the container
   */
  className?: string;
}

/**
 * Status configuration mapping for visual representation
 */
interface StatusConfig {
  label: string;
  color: string;
  bgColor: string;
  icon: React.ReactNode;
  description: string;
}

/**
 * PaymentStatusIndicator component
 * 
 * A component that visually represents payment transaction status through color-coded icons and labels.
 * It ensures consistent status representation across the application by mapping status codes to
 * appropriate visual indicators for successful, pending, failed, and other payment states.
 */
const PaymentStatusIndicator: React.FC<PaymentStatusIndicatorProps> = ({
  status,
  showLabel = true,
  size = 'md',
  className = ''
}) => {
  // Status configuration mapping with visual properties for each status
  const statusConfig: Record<PaymentStatus, StatusConfig> = {
    CREATED: {
      label: 'Created',
      color: 'text-gray-600 dark:text-gray-400',
      bgColor: 'bg-gray-100 dark:bg-gray-700',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path d="M5.433 13.917l1.262-3.155A4 4 0 017.58 9.42l6.92-6.918a2.121 2.121 0 013 3l-6.92 6.918c-.383.383-.84.685-1.343.886l-3.154 1.262a.5.5 0 01-.65-.65z" />
          <path d="M3.5 5.75c0-.69.56-1.25 1.25-1.25H10A.75.75 0 0010 3H4.75A2.75 2.75 0 002 5.75v9.5A2.75 2.75 0 004.75 18h9.5A2.75 2.75 0 0017 15.25V10a.75.75 0 00-1.5 0v5.25c0 .69-.56 1.25-1.25 1.25h-9.5c-.69 0-1.25-.56-1.25-1.25v-9.5z" />
        </svg>
      ),
      description: 'Transaction has been created but not yet processed'
    },
    PROCESSING: {
      label: 'Processing',
      color: 'text-blue-600 dark:text-blue-400',
      bgColor: 'bg-blue-100 dark:bg-blue-900/30',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full animate-spin">
          <path fillRule="evenodd" d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H3.989a.75.75 0 00-.75.75v4.242a.75.75 0 001.5 0v-2.43l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.39zm1.23-3.723a.75.75 0 00.219-.53V2.929a.75.75 0 00-1.5 0V5.36l-.31-.31A7 7 0 003.239 8.188a.75.75 0 101.448.389A5.5 5.5 0 0113.89 6.11l.311.31h-2.432a.75.75 0 000 1.5h4.243a.75.75 0 00.53-.219z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Transaction is being processed'
    },
    AUTHORIZED: {
      label: 'Authorized',
      color: 'text-purple-600 dark:text-purple-400',
      bgColor: 'bg-purple-100 dark:bg-purple-900/30',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Transaction has been authorized but not yet captured'
    },
    CAPTURED: {
      label: 'Complete',
      color: 'text-green-600 dark:text-green-400',
      bgColor: 'bg-green-100 dark:bg-green-900/30',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Funds have been captured successfully'
    },
    REFUNDED: {
      label: 'Refunded',
      color: 'text-amber-600 dark:text-amber-400',
      bgColor: 'bg-amber-100 dark:bg-amber-900/30',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path fillRule="evenodd" d="M5.5 10a.75.75 0 01.75-.75h6.5a.75.75 0 010 1.5h-6.5a.75.75 0 01-.75-.75zm-2.72-5.171A6.973 6.973 0 0110 2a6.973 6.973 0 017.22 2.829c.329.5.078 1.171-.422 1.5A1.077 1.077 0 0116 6.22a.996.996 0 01-.869-.513A4.977 4.977 0 0010 4a4.977 4.977 0 00-5.131 1.707.996.996 0 01-.869.513 1.077 1.077 0 01-.798-.109c-.5-.329-.75-1-.422-1.5zM10 18a6.977 6.977 0 01-5.13-2.229 1 1 0 111.46-1.368A4.977 4.977 0 0010 16a4.977 4.977 0 005.13-1.597 1 1 0 111.46 1.368A6.977 6.977 0 0110 18z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Transaction has been refunded'
    },
    FAILED: {
      label: 'Failed',
      color: 'text-red-600 dark:text-red-400',
      bgColor: 'bg-red-100 dark:bg-red-900/30',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Transaction processing has failed'
    },
    VOIDED: {
      label: 'Voided',
      color: 'text-gray-600 dark:text-gray-400',
      bgColor: 'bg-gray-100 dark:bg-gray-700',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-full h-full">
          <path fillRule="evenodd" d="M4 10a.75.75 0 01.75-.75h10.5a.75.75 0 010 1.5H4.75A.75.75 0 014 10z" clipRule="evenodd" />
        </svg>
      ),
      description: 'Transaction has been voided'
    }
  };

  // Get the configuration for the current status
  const config = statusConfig[status] || statusConfig.PROCESSING;
  
  // Determine size classes
  const sizeClasses = {
    sm: {
      container: 'text-xs',
      icon: 'w-3 h-3',
      pill: 'px-2 py-0.5'
    },
    md: {
      container: 'text-sm',
      icon: 'w-4 h-4',
      pill: 'px-2.5 py-0.5'
    },
    lg: {
      container: 'text-base',
      icon: 'w-5 h-5',
      pill: 'px-3 py-1'
    }
  }[size];

  return (
    <div 
      className={`inline-flex items-center ${className}`}
      data-testid="payment-status-indicator"
    >
      {showLabel ? (
        // Pill style with icon and label
        <span 
          className={`inline-flex items-center gap-1.5 rounded-full font-medium ${config.color} ${config.bgColor} ${sizeClasses.pill} ${sizeClasses.container}`}
          aria-label={`Payment status: ${config.label} - ${config.description}`}
          title={config.description}
        >
          <span className={sizeClasses.icon}>
            {config.icon}
          </span>
          {config.label}
        </span>
      ) : (
        // Icon only
        <span 
          className={`inline-flex items-center justify-center rounded-full ${config.color} ${config.bgColor} p-1`}
          style={{ width: size === 'sm' ? '20px' : size === 'md' ? '24px' : '28px', height: size === 'sm' ? '20px' : size === 'md' ? '24px' : '28px' }}
          aria-label={`Payment status: ${config.label} - ${config.description}`}
          title={config.description}
        >
          <span className={`${size === 'sm' ? 'w-3 h-3' : size === 'md' ? 'w-4 h-4' : 'w-5 h-5'}`}>
            {config.icon}
          </span>
        </span>
      )}
    </div>
  );
};

export default PaymentStatusIndicator;