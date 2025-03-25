import React, { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { format } from 'date-fns';

type PaymentStatus = 'AUTHORIZED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REFUNDED' | 'VOIDED';

interface Merchant {
  id: string;
  name: string;
}

interface FilterValues {
  dateFrom: string;
  dateTo: string;
  amountMin: string;
  amountMax: string;
  statuses: PaymentStatus[];
  merchantId: string;
}

interface PaymentFilterPanelProps {
  onFilterChange: (filters: FilterValues) => void;
  merchants?: Merchant[];
  initialFilters?: Partial<FilterValues>;
  className?: string;
}

const PaymentFilterPanel: React.FC<PaymentFilterPanelProps> = ({
  onFilterChange,
  merchants = [],
  initialFilters = {},
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FilterValues>({
    defaultValues: {
      dateFrom: initialFilters.dateFrom || '',
      dateTo: initialFilters.dateTo || '',
      amountMin: initialFilters.amountMin || '',
      amountMax: initialFilters.amountMax || '',
      statuses: initialFilters.statuses || [],
      merchantId: initialFilters.merchantId || '',
    }
  });

  // Status options for the filter
  const statusOptions: { value: PaymentStatus; label: string }[] = [
    { value: 'AUTHORIZED', label: 'Authorized' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'REFUNDED', label: 'Refunded' },
    { value: 'VOIDED', label: 'Voided' },
  ];

  // Reset filters to initial values
  const resetFilters = () => {
    reset({
      dateFrom: '',
      dateTo: '',
      amountMin: '',
      amountMax: '',
      statuses: [],
      merchantId: '',
    });
    
    // Trigger filter change with empty values
    onFilterChange({
      dateFrom: '',
      dateTo: '',
      amountMin: '',
      amountMax: '',
      statuses: [],
      merchantId: '',
    });
  };

  // Apply filters
  const applyFilters = (data: FilterValues) => {
    onFilterChange(data);
    
    // On mobile, close the filter panel after applying
    if (window.innerWidth < 640) {
      setIsOpen(false);
    }
  };

  // Format date for display
  const formatDate = (dateString: string): string => {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);
      return format(date, 'yyyy-MM-dd');
    } catch (error) {
      return dateString;
    }
  };

  // Toggle filter panel visibility on mobile
  const toggleFilterPanel = () => {
    setIsOpen(!isOpen);
  };

  // Update filters when initialFilters change
  useEffect(() => {
    if (initialFilters) {
      reset({
        dateFrom: initialFilters.dateFrom || '',
        dateTo: initialFilters.dateTo || '',
        amountMin: initialFilters.amountMin || '',
        amountMax: initialFilters.amountMax || '',
        statuses: initialFilters.statuses || [],
        merchantId: initialFilters.merchantId || '',
      });
    }
  }, [initialFilters, reset]);

  return (
    <div className={`payment-filter-panel ${className}`}>
      {/* Mobile filter toggle button */}
      <button 
        className="sm:hidden w-full flex items-center justify-between bg-white dark:bg-gray-800 p-4 border-b border-gray-200 dark:border-gray-700"
        onClick={toggleFilterPanel}
        aria-expanded={isOpen}
        aria-controls="payment-filters"
      >
        <span className="font-medium">Filters</span>
        <svg 
          className={`w-5 h-5 transition-transform ${isOpen ? 'transform rotate-180' : ''}`} 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24" 
          xmlns="http://www.w3.org/2000/svg"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Filter panel content - always visible on desktop, toggleable on mobile */}
      <div 
        id="payment-filters"
        className={`
          bg-white dark:bg-gray-800 p-4 border rounded-md shadow-sm
          sm:block sm:h-auto sm:opacity-100 sm:visible sm:static
          ${isOpen ? 'block' : 'hidden'}
          transition-all duration-300 ease-in-out
        `}
      >
        <form onSubmit={handleSubmit(applyFilters)}>
          <div className="space-y-4">
            {/* Date Range Filter */}
            <div className="filter-section">
              <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Date Range</h3>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label htmlFor="dateFrom" className="sr-only">From</label>
                  <input
                    id="dateFrom"
                    type="date"
                    {...register('dateFrom')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                    placeholder="From"
                  />
                </div>
                <div>
                  <label htmlFor="dateTo" className="sr-only">To</label>
                  <input
                    id="dateTo"
                    type="date"
                    {...register('dateTo')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                    placeholder="To"
                  />
                </div>
              </div>
            </div>

            {/* Amount Range Filter */}
            <div className="filter-section">
              <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Amount Range</h3>
              <div className="grid grid-cols-2 gap-2">
                <div className="relative">
                  <label htmlFor="amountMin" className="sr-only">Min Amount</label>
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <span className="text-gray-500 dark:text-gray-400">$</span>
                  </div>
                  <input
                    id="amountMin"
                    type="number"
                    step="0.01"
                    min="0"
                    {...register('amountMin')}
                    className="w-full pl-7 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                    placeholder="Min"
                  />
                </div>
                <div className="relative">
                  <label htmlFor="amountMax" className="sr-only">Max Amount</label>
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <span className="text-gray-500 dark:text-gray-400">$</span>
                  </div>
                  <input
                    id="amountMax"
                    type="number"
                    step="0.01"
                    min="0"
                    {...register('amountMax')}
                    className="w-full pl-7 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                    placeholder="Max"
                  />
                </div>
              </div>
            </div>

            {/* Status Filter */}
            <div className="filter-section">
              <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Status</h3>
              <div className="space-y-2">
                <Controller
                  name="statuses"
                  control={control}
                  render={({ field }) => (
                    <div className="grid grid-cols-2 gap-2">
                      {statusOptions.map((status) => (
                        <label key={status.value} className="inline-flex items-center">
                          <input
                            type="checkbox"
                            value={status.value}
                            checked={field.value?.includes(status.value)}
                            onChange={(e) => {
                              const value = status.value;
                              const newValues = e.target.checked
                                ? [...(field.value || []), value]
                                : (field.value || []).filter((val) => val !== value);
                              field.onChange(newValues);
                            }}
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                          />
                          <span className="ml-2 text-sm text-gray-700 dark:text-gray-300">{status.label}</span>
                        </label>
                      ))}
                    </div>
                  )}
                />
              </div>
            </div>

            {/* Merchant Filter */}
            {merchants.length > 0 && (
              <div className="filter-section">
                <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Merchant</h3>
                <select
                  {...register('merchantId')}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">All Merchants</option>
                  {merchants.map((merchant) => (
                    <option key={merchant.id} value={merchant.id}>
                      {merchant.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Filter Actions */}
            <div className="flex space-x-2 pt-2">
              <button
                type="submit"
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-md text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
              >
                Apply Filters
              </button>
              <button
                type="button"
                onClick={resetFilters}
                className="flex-1 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 py-2 px-4 rounded-md text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors"
              >
                Reset
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PaymentFilterPanel;