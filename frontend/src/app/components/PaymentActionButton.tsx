import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Dialog, Transition } from '@headlessui/react';
import { Fragment } from 'react';

/**
 * Payment action types supported by the component
 */
export type PaymentActionType = 
  | 'authorize' 
  | 'capture' 
  | 'refund' 
  | 'void' 
  | 'view';

/**
 * Payment transaction status types
 */
export type PaymentStatusType = 
  | 'pending' 
  | 'authorized' 
  | 'processing' 
  | 'completed' 
  | 'captured' 
  | 'failed' 
  | 'voided' 
  | 'refunded' 
  | 'partially_refunded' 
  | 'partially_captured';

/**
 * Props for the PaymentActionButton component
 */
interface PaymentActionButtonProps {
  /** The type of action this button performs */
  actionType: PaymentActionType;
  
  /** Current status of the payment transaction */
  paymentStatus: PaymentStatusType;
  
  /** Transaction ID for the payment */
  transactionId: string;
  
  /** Organization ID for permission context */
  organizationId: string;
  
  /** Account ID for permission context */
  accountId: string;
  
  /** Optional amount for display in confirmation dialogs */
  amount?: string;
  
  /** Optional currency code for display in confirmation dialogs */
  currency?: string;
  
  /** Optional callback function when action completes successfully */
  onActionComplete?: () => void;
  
  /** Optional custom button text */
  buttonText?: string;
  
  /** Optional custom button class name */
  className?: string;
  
  /** Optional flag to disable the button */
  disabled?: boolean;
  
  /** Optional flag to show loading state */
  loading?: boolean;
  
  /** Optional flag to skip confirmation dialog (use with caution) */
  skipConfirmation?: boolean;
  
  /** Optional user permissions array */
  userPermissions?: string[];
}

/**
 * A specialized button component for payment-related actions such as capture, refund, and void operations.
 * Implements consistent styling, permission-based visibility, and confirmation flows for financial operations.
 */
const PaymentActionButton: React.FC<PaymentActionButtonProps> = ({
  actionType,
  paymentStatus,
  transactionId,
  organizationId,
  accountId,
  amount,
  currency,
  onActionComplete,
  buttonText,
  className = '',
  disabled = false,
  loading = false,
  skipConfirmation = false,
  userPermissions = [],
}) => {
  const navigate = useNavigate();
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  
  // Define action-specific properties
  const actionConfig = {
    authorize: {
      text: buttonText || 'Authorize Payment',
      confirmMessage: 'Are you sure you want to authorize this payment?',
      permission: 'payment:authorize',
      destructive: false,
      route: `/payments/${organizationId}/${accountId}/authorize/${transactionId}`,
      validStatuses: ['pending'],
      className: 'bg-blue-600 hover:bg-blue-700 text-white',
    },
    capture: {
      text: buttonText || 'Capture Funds',
      confirmMessage: `Are you sure you want to capture ${amount ? `${amount} ${currency}` : 'funds'} for this transaction?`,
      permission: 'payment:capture',
      destructive: false,
      route: `/payments/${organizationId}/${accountId}/capture/${transactionId}`,
      validStatuses: ['authorized', 'partially_captured'],
      className: 'bg-green-600 hover:bg-green-700 text-white',
    },
    refund: {
      text: buttonText || 'Issue Refund',
      confirmMessage: `Are you sure you want to refund ${amount ? `${amount} ${currency}` : 'this transaction'}?`,
      permission: 'payment:refund',
      destructive: true,
      route: `/payments/${organizationId}/${accountId}/refund/${transactionId}`,
      validStatuses: ['captured', 'completed', 'partially_refunded'],
      className: 'bg-amber-600 hover:bg-amber-700 text-white',
    },
    void: {
      text: buttonText || 'Void Transaction',
      confirmMessage: 'Are you sure you want to void this transaction? This action cannot be undone.',
      permission: 'payment:void',
      destructive: true,
      route: `/payments/${organizationId}/${accountId}/void/${transactionId}`,
      validStatuses: ['authorized', 'pending'],
      className: 'bg-red-600 hover:bg-red-700 text-white',
    },
    view: {
      text: buttonText || 'View Details',
      confirmMessage: '',
      permission: 'payment:view',
      destructive: false,
      route: `/payments/${organizationId}/${accountId}/transaction/${transactionId}`,
      validStatuses: ['pending', 'authorized', 'processing', 'completed', 'captured', 'failed', 'voided', 'refunded', 'partially_refunded', 'partially_captured'],
      className: 'bg-gray-600 hover:bg-gray-700 text-white',
    },
  };
  
  const config = actionConfig[actionType];
  
  /**
   * Check if the user has permission to perform this action
   */
  const hasPermission = (): boolean => {
    // If no permissions are provided, assume public action
    if (!userPermissions || userPermissions.length === 0) {
      return true;
    }
    
    return userPermissions.includes(config.permission);
  };
  
  /**
   * Check if the action is valid for the current payment status
   */
  const isValidForStatus = (): boolean => {
    return config.validStatuses.includes(paymentStatus);
  };
  
  /**
   * Handle button click based on action type
   */
  const handleClick = () => {
    // For view actions or when confirmation is skipped, navigate directly
    if (actionType === 'view' || skipConfirmation) {
      navigate(config.route);
      return;
    }
    
    // For other actions, show confirmation dialog
    setIsConfirmOpen(true);
  };
  
  /**
   * Handle confirmation dialog confirmation
   */
  const handleConfirm = async () => {
    setIsProcessing(true);
    
    try {
      // For destructive actions that require additional verification,
      // we would typically make an API call here to verify permissions
      // before proceeding with the navigation
      
      // Navigate to the action-specific page
      navigate(config.route);
      
      // Call the completion callback if provided
      if (onActionComplete) {
        onActionComplete();
      }
    } catch (error) {
      console.error('Error processing payment action:', error);
    } finally {
      setIsProcessing(false);
      setIsConfirmOpen(false);
    }
  };
  
  /**
   * Close the confirmation dialog
   */
  const closeConfirmDialog = () => {
    setIsConfirmOpen(false);
  };
  
  // If user doesn't have permission or status is invalid, don't render the button
  if (!hasPermission() || !isValidForStatus()) {
    return null;
  }
  
  return (
    <>
      <button
        type="button"
        onClick={handleClick}
        disabled={disabled || loading || isProcessing}
        className={`
          relative inline-flex items-center justify-center
          px-4 py-2 min-h-[44px] min-w-[44px] rounded-md
          font-medium text-sm shadow-sm
          focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
          transition-colors duration-200 ease-in-out
          mx-2 my-1 first:ml-0 last:mr-0
          ${config.className}
          ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
          ${className}
        `}
        aria-label={config.text}
      >
        {/* Loading spinner */}
        {(loading || isProcessing) && (
          <svg 
            className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" 
            xmlns="http://www.w3.org/2000/svg" 
            fill="none" 
            viewBox="0 0 24 24"
          >
            <circle 
              className="opacity-25" 
              cx="12" 
              cy="12" 
              r="10" 
              stroke="currentColor" 
              strokeWidth="4"
            />
            <path 
              className="opacity-75" 
              fill="currentColor" 
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        
        {/* Button text */}
        <span>{config.text}</span>
      </button>
      
      {/* Confirmation Dialog */}
      <Transition appear show={isConfirmOpen} as={Fragment}>
        <Dialog as="div" className="relative z-10" onClose={closeConfirmDialog}>
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <div className="fixed inset-0 bg-black bg-opacity-25" />
          </Transition.Child>

          <div className="fixed inset-0 overflow-y-auto">
            <div className="flex min-h-full items-center justify-center p-4 text-center">
              <Transition.Child
                as={Fragment}
                enter="ease-out duration-300"
                enterFrom="opacity-0 scale-95"
                enterTo="opacity-100 scale-100"
                leave="ease-in duration-200"
                leaveFrom="opacity-100 scale-100"
                leaveTo="opacity-0 scale-95"
              >
                <Dialog.Panel className="w-full max-w-md transform overflow-hidden rounded-2xl bg-white dark:bg-gray-800 p-6 text-left align-middle shadow-xl transition-all">
                  <Dialog.Title
                    as="h3"
                    className="text-lg font-medium leading-6 text-gray-900 dark:text-white"
                  >
                    {config.destructive ? 'Confirm Critical Action' : 'Confirm Action'}
                  </Dialog.Title>
                  
                  <div className="mt-2">
                    <p className="text-sm text-gray-500 dark:text-gray-300">
                      {config.confirmMessage}
                    </p>
                    
                    {/* Transaction details for reference */}
                    <div className="mt-3 p-3 bg-gray-100 dark:bg-gray-700 rounded-md">
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        Transaction ID: {transactionId}
                      </p>
                      {amount && currency && (
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                          Amount: {amount} {currency}
                        </p>
                      )}
                    </div>
                    
                    {/* Additional security verification for destructive actions */}
                    {config.destructive && (
                      <div className="mt-3 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
                        <p className="text-xs text-red-600 dark:text-red-400">
                          This action cannot be undone. Please confirm that you want to proceed.
                        </p>
                      </div>
                    )}
                  </div>

                  <div className="mt-6 flex justify-end space-x-3">
                    <button
                      type="button"
                      className="inline-flex justify-center rounded-md border border-transparent bg-gray-200 dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-900 dark:text-gray-100 hover:bg-gray-300 dark:hover:bg-gray-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-gray-500 focus-visible:ring-offset-2"
                      onClick={closeConfirmDialog}
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      className={`inline-flex justify-center rounded-md border border-transparent px-4 py-2 text-sm font-medium text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 ${
                        config.destructive 
                          ? 'bg-red-600 hover:bg-red-700 focus-visible:ring-red-500' 
                          : 'bg-blue-600 hover:bg-blue-700 focus-visible:ring-blue-500'
                      } ${isProcessing ? 'opacity-75 cursor-wait' : ''}`}
                      onClick={handleConfirm}
                      disabled={isProcessing}
                    >
                      {isProcessing ? (
                        <>
                          <svg 
                            className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" 
                            xmlns="http://www.w3.org/2000/svg" 
                            fill="none" 
                            viewBox="0 0 24 24"
                          >
                            <circle 
                              className="opacity-25" 
                              cx="12" 
                              cy="12" 
                              r="10" 
                              stroke="currentColor" 
                              strokeWidth="4"
                            />
                            <path 
                              className="opacity-75" 
                              fill="currentColor" 
                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                            />
                          </svg>
                          Processing...
                        </>
                      ) : (
                        config.destructive ? 'Confirm' : 'Continue'
                      )}
                    </button>
                  </div>
                </Dialog.Panel>
              </Transition.Child>
            </div>
          </div>
        </Dialog>
      </Transition>
    </>
  );
};

export default PaymentActionButton;