import React, { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { format } from 'date-fns';
import { useDispatch, useSelector } from 'react-redux';
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../Main';

// Define filter state types
export interface PaymentFilterState {
  dateRange: {
    startDate: string | null;
    endDate: string | null;
  };
  status: string[];
  amountRange: {
    min: number | null;
    max: number | null;
  };
  merchantId: string | null;
  isFilterActive: boolean;
}

// Create a slice for payment filters
const initialState: PaymentFilterState = {
  dateRange: {
    startDate: null,
    endDate: null,
  },
  status: [],
  amountRange: {
    min: null,
    max: null,
  },
  merchantId: null,
  isFilterActive: false,
};

// This would normally be in a separate file (redux/payments/filterSlice.ts)
// Included here for completeness since we're creating this component
export const filterSlice = createSlice({
  name: 'paymentFilters',
  initialState,
  reducers: {
    setDateRange: (state, action: PayloadAction<{ startDate: string | null; endDate: string | null }>) => {
      state.dateRange = action.payload;
      state.isFilterActive = true;
    },
    setStatus: (state, action: PayloadAction<string[]>) => {
      state.status = action.payload;
      state.isFilterActive = Boolean(action.payload.length);
    },
    setAmountRange: (state, action: PayloadAction<{ min: number | null; max: number | null }>) => {
      state.amountRange = action.payload;
      state.isFilterActive = Boolean(action.payload.min || action.payload.max);
    },
    setMerchantId: (state, action: PayloadAction<string | null>) => {
      state.merchantId = action.payload;
      state.isFilterActive = Boolean(action.payload);
    },
    resetFilters: (state) => {
      return initialState;
    },
  },
});

export const { setDateRange, setStatus, setAmountRange, setMerchantId, resetFilters } = filterSlice.actions;

// Define available payment statuses
const PAYMENT_STATUSES = [
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'AUTHORIZED', label: 'Authorized' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'REFUNDED', label: 'Refunded' },
  { value: 'PARTIALLY_REFUNDED', label: 'Partially Refunded' },
];

// Mock merchant data - in a real app, this would come from an API
const MERCHANTS = [
  { id: 'merchant-1', name: 'Acme Corporation' },
  { id: 'merchant-2', name: 'Globex Industries' },
  { id: 'merchant-3', name: 'Initech LLC' },
  { id: 'merchant-4', name: 'Umbrella Corp' },
];

interface PaymentFilterPanelProps {
  onApplyFilters?: (filters: PaymentFilterState) => void;
}

/**
 * PaymentFilterPanel Component
 * 
 * A responsive filter panel for payment transactions that adapts between desktop and mobile views.
 * On desktop, it appears as a fixed sidebar. On mobile, it collapses into an expandable drawer.
 * 
 * Features:
 * - Date range selection
 * - Payment status multi-select
 * - Amount range inputs (min/max)
 * - Merchant selection
 * - Responsive design with mobile drawer pattern
 * - Integration with Redux for filter state management
 */
const PaymentFilterPanel: React.FC<PaymentFilterPanelProps> = ({ onApplyFilters }) => {
  // State for mobile drawer visibility
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  
  // Get current filter state from Redux
  const filterState = useSelector((state: RootState) => state.paymentFilters);
  const dispatch = useDispatch();
  
  // Set up form with React Hook Form
  const { control, handleSubmit, reset, formState: { isDirty } } = useForm<PaymentFilterState>({
    defaultValues: filterState,
  });
  
  // Reset form when filter state changes externally
  useEffect(() => {
    reset(filterState);
  }, [filterState, reset]);
  
  // Handle form submission
  const onSubmit = (data: PaymentFilterState) => {
    // Update Redux state
    dispatch(setDateRange(data.dateRange));
    dispatch(setStatus(data.status));
    dispatch(setAmountRange(data.amountRange));
    dispatch(setMerchantId(data.merchantId));
    
    // Close drawer on mobile after applying filters
    setIsDrawerOpen(false);
    
    // Call external handler if provided
    if (onApplyFilters) {
      onApplyFilters(data);
    }
  };
  
  // Handle filter reset
  const handleReset = () => {
    dispatch(resetFilters());
    reset(initialState);
  };
  
  // Toggle drawer for mobile view
  const toggleDrawer = () => {
    setIsDrawerOpen(!isDrawerOpen);
  };
  
  // Determine if any filters are active
  const hasActiveFilters = filterState.isFilterActive;
  
  // Filter panel content - shared between desktop and mobile views
  const filterPanelContent = (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      {/* Date Range Filter */}
      <div className="space-y-2">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">Date Range</h3>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="startDate" className="block text-xs text-gray-500 dark:text-gray-400">
              From
            </label>
            <Controller
              name="dateRange.startDate"
              control={control}
              render={({ field }) => (
                <input
                  id="startDate"
                  type="date"
                  className="w-full px-3 py-2 text-sm border rounded-md border-gray-300 dark:border-gray-700 
                             bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 
                             focus:ring-blue-500 focus:border-transparent"
                  {...field}
                  value={field.value || ''}
                />
              )}
            />
          </div>
          <div>
            <label htmlFor="endDate" className="block text-xs text-gray-500 dark:text-gray-400">
              To
            </label>
            <Controller
              name="dateRange.endDate"
              control={control}
              render={({ field }) => (
                <input
                  id="endDate"
                  type="date"
                  className="w-full px-3 py-2 text-sm border rounded-md border-gray-300 dark:border-gray-700 
                             bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 
                             focus:ring-blue-500 focus:border-transparent"
                  {...field}
                  value={field.value || ''}
                />
              )}
            />
          </div>
        </div>
      </div>
      
      {/* Status Filter */}
      <div className="space-y-2">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">Status</h3>
        <div className="grid grid-cols-2 gap-2">
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <>
                {PAYMENT_STATUSES.map((status) => (
                  <div key={status.value} className="flex items-center">
                    <input
                      id={`status-${status.value}`}
                      type="checkbox"
                      className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 
                                 dark:border-gray-600 dark:bg-gray-700"
                      value={status.value}
                      checked={field.value?.includes(status.value)}
                      onChange={(e) => {
                        const newValue = e.target.checked
                          ? [...(field.value || []), status.value]
                          : (field.value || []).filter(v => v !== status.value);
                        field.onChange(newValue);
                      }}
                    />
                    <label
                      htmlFor={`status-${status.value}`}
                      className="ml-2 text-sm text-gray-700 dark:text-gray-300"
                    >
                      {status.label}
                    </label>
                  </div>
                ))}
              </>
            )}
          />
        </div>
      </div>
      
      {/* Amount Range Filter */}
      <div className="space-y-2">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">Amount Range</h3>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="minAmount" className="block text-xs text-gray-500 dark:text-gray-400">
              Min ($)
            </label>
            <Controller
              name="amountRange.min"
              control={control}
              render={({ field }) => (
                <input
                  id="minAmount"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  className="w-full px-3 py-2 text-sm border rounded-md border-gray-300 dark:border-gray-700 
                             bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 
                             focus:ring-blue-500 focus:border-transparent"
                  {...field}
                  value={field.value === null ? '' : field.value}
                  onChange={(e) => field.onChange(e.target.value === '' ? null : Number(e.target.value))}
                />
              )}
            />
          </div>
          <div>
            <label htmlFor="maxAmount" className="block text-xs text-gray-500 dark:text-gray-400">
              Max ($)
            </label>
            <Controller
              name="amountRange.max"
              control={control}
              render={({ field }) => (
                <input
                  id="maxAmount"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  className="w-full px-3 py-2 text-sm border rounded-md border-gray-300 dark:border-gray-700 
                             bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 
                             focus:ring-blue-500 focus:border-transparent"
                  {...field}
                  value={field.value === null ? '' : field.value}
                  onChange={(e) => field.onChange(e.target.value === '' ? null : Number(e.target.value))}
                />
              )}
            />
          </div>
        </div>
      </div>
      
      {/* Merchant Filter */}
      <div className="space-y-2">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300">Merchant</h3>
        <Controller
          name="merchantId"
          control={control}
          render={({ field }) => (
            <select
              className="w-full px-3 py-2 text-sm border rounded-md border-gray-300 dark:border-gray-700 
                         bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 
                         focus:ring-blue-500 focus:border-transparent"
              {...field}
              value={field.value || ''}
              onChange={(e) => field.onChange(e.target.value || null)}
            >
              <option value="">All Merchants</option>
              {MERCHANTS.map((merchant) => (
                <option key={merchant.id} value={merchant.id}>
                  {merchant.name}
                </option>
              ))}
            </select>
          )}
        />
      </div>
      
      {/* Action Buttons */}
      <div className="flex flex-col gap-2 mt-4">
        <button
          type="submit"
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 
                     focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 
                     dark:bg-blue-700 dark:hover:bg-blue-800"
        >
          Apply Filters
        </button>
        <button
          type="button"
          onClick={handleReset}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 
                     rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 
                     focus:ring-blue-500 dark:bg-gray-800 dark:text-gray-200 dark:border-gray-600 
                     dark:hover:bg-gray-700"
        >
          Reset
        </button>
      </div>
    </form>
  );
  
  return (
    <>
      {/* Desktop Filter Panel - Fixed sidebar */}
      <div className="hidden sm:block w-64 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-medium text-gray-900 dark:text-white">Filters</h2>
          {hasActiveFilters && (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
              Active
            </span>
          )}
        </div>
        {filterPanelContent}
      </div>
      
      {/* Mobile Filter Button - Fixed at top */}
      <div className="sm:hidden sticky top-0 z-10 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 p-4">
        <button
          type="button"
          onClick={toggleDrawer}
          className="flex items-center justify-between w-full px-4 py-2 text-sm font-medium text-gray-700 
                     bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none 
                     focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 dark:bg-gray-800 
                     dark:text-gray-200 dark:border-gray-600 dark:hover:bg-gray-700"
        >
          <span className="flex items-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="w-5 h-5 mr-2 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
              />
            </svg>
            Filters
          </span>
          {hasActiveFilters && (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
              Active
            </span>
          )}
        </button>
      </div>
      
      {/* Mobile Filter Drawer - Expandable panel */}
      {isDrawerOpen && (
        <div className="sm:hidden fixed inset-0 z-20 overflow-hidden">
          {/* Backdrop */}
          <div 
            className="absolute inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
            onClick={toggleDrawer}
          />
          
          {/* Drawer Panel */}
          <div className="absolute inset-y-0 right-0 max-w-full flex">
            <div className="relative w-screen max-w-md">
              <div className="h-full flex flex-col bg-white dark:bg-gray-900 shadow-xl overflow-y-auto">
                {/* Drawer Header */}
                <div className="px-4 py-6 bg-gray-50 dark:bg-gray-800 sm:px-6">
                  <div className="flex items-center justify-between">
                    <h2 className="text-lg font-medium text-gray-900 dark:text-white">Filters</h2>
                    <button
                      type="button"
                      onClick={toggleDrawer}
                      className="text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <span className="sr-only">Close panel</span>
                      <svg
                        className="h-6 w-6"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        aria-hidden="true"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth="2"
                          d="M6 18L18 6M6 6l12 12"
                        />
                      </svg>
                    </button>
                  </div>
                </div>
                
                {/* Drawer Content */}
                <div className="flex-1 px-4 py-6 sm:px-6">
                  {filterPanelContent}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default PaymentFilterPanel;