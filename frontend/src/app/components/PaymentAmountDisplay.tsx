import React, { useMemo } from 'react';

/**
 * Currency formatting options for different currency types
 */
interface CurrencyFormatOptions {
  symbol: string;
  symbolPosition: 'prefix' | 'suffix';
  decimalPlaces: number;
  thousandsSeparator: string;
  decimalSeparator: string;
  spaceBetweenAmountAndSymbol: boolean;
}

/**
 * Default currency format configurations
 */
const CURRENCY_FORMATS: Record<string, CurrencyFormatOptions> = {
  USD: {
    symbol: '$',
    symbolPosition: 'prefix',
    decimalPlaces: 2,
    thousandsSeparator: ',',
    decimalSeparator: '.',
    spaceBetweenAmountAndSymbol: false
  },
  EUR: {
    symbol: '€',
    symbolPosition: 'prefix',
    decimalPlaces: 2,
    thousandsSeparator: '.',
    decimalSeparator: ',',
    spaceBetweenAmountAndSymbol: true
  },
  GBP: {
    symbol: '£',
    symbolPosition: 'prefix',
    decimalPlaces: 2,
    thousandsSeparator: ',',
    decimalSeparator: '.',
    spaceBetweenAmountAndSymbol: false
  },
  JPY: {
    symbol: '¥',
    symbolPosition: 'prefix',
    decimalPlaces: 0,
    thousandsSeparator: ',',
    decimalSeparator: '.',
    spaceBetweenAmountAndSymbol: false
  },
  AUD: {
    symbol: 'AUD',
    symbolPosition: 'suffix',
    decimalPlaces: 2,
    thousandsSeparator: ',',
    decimalSeparator: '.',
    spaceBetweenAmountAndSymbol: true
  },
  CAD: {
    symbol: 'CAD',
    symbolPosition: 'suffix',
    decimalPlaces: 2,
    thousandsSeparator: ',',
    decimalSeparator: '.',
    spaceBetweenAmountAndSymbol: true
  },
  // Add more currencies as needed
};

/**
 * Props for the PaymentAmountDisplay component
 */
interface PaymentAmountDisplayProps {
  /**
   * The amount to display
   */
  amount: number;
  
  /**
   * The currency code (ISO 4217)
   */
  currency: string;
  
  /**
   * Optional CSS class name for styling
   */
  className?: string;
  
  /**
   * Optional flag to show positive amounts with a plus sign
   */
  showPositiveSign?: boolean;
  
  /**
   * Optional flag to highlight negative amounts
   */
  highlightNegative?: boolean;
  
  /**
   * Optional custom format options to override defaults
   */
  formatOptions?: Partial<CurrencyFormatOptions>;
  
  /**
   * Optional flag to use compact notation for large numbers
   */
  useCompactNotation?: boolean;
  
  /**
   * Optional locale for internationalization
   */
  locale?: string;
}

/**
 * A component for displaying payment amounts with proper currency formatting.
 * Handles different currency symbols, decimal precision, and internationalization.
 */
const PaymentAmountDisplay: React.FC<PaymentAmountDisplayProps> = ({
  amount,
  currency,
  className = '',
  showPositiveSign = false,
  highlightNegative = true,
  formatOptions,
  useCompactNotation = false,
  locale = 'en-US'
}) => {
  // Determine if the amount is negative
  const isNegative = amount < 0;
  
  // Get the absolute value for formatting
  const absoluteAmount = Math.abs(amount);
  
  // Get currency format options, with custom overrides if provided
  const currencyFormat = useMemo(() => {
    // Get default format for the currency, or use USD as fallback
    const defaultFormat = CURRENCY_FORMATS[currency] || CURRENCY_FORMATS.USD;
    
    // Merge with any custom format options
    return {
      ...defaultFormat,
      ...(formatOptions || {})
    };
  }, [currency, formatOptions]);
  
  // Format the amount according to the currency rules
  const formattedAmount = useMemo(() => {
    try {
      // For compact notation (e.g., 1.2M, 5K)
      if (useCompactNotation) {
        return new Intl.NumberFormat(locale, {
          notation: 'compact',
          maximumFractionDigits: currencyFormat.decimalPlaces
        }).format(absoluteAmount);
      }
      
      // For standard notation with proper decimal and thousands separators
      const parts = absoluteAmount.toFixed(currencyFormat.decimalPlaces).split('.');
      
      // Format the integer part with thousands separators
      const integerPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, currencyFormat.thousandsSeparator);
      
      // Combine with decimal part if it exists
      const formattedNumber = parts.length > 1 
        ? `${integerPart}${currencyFormat.decimalSeparator}${parts[1]}`
        : integerPart;
        
      return formattedNumber;
    } catch (error) {
      console.error('Error formatting amount:', error);
      return absoluteAmount.toString();
    }
  }, [absoluteAmount, currencyFormat, locale, useCompactNotation]);
  
  // Construct the final display with currency symbol and sign
  const displayAmount = useMemo(() => {
    // Determine sign prefix
    const signPrefix = isNegative 
      ? '-' 
      : (showPositiveSign ? '+' : '');
    
    // Add space between symbol and amount if configured
    const space = currencyFormat.spaceBetweenAmountAndSymbol ? ' ' : '';
    
    // Construct final string based on symbol position
    if (currencyFormat.symbolPosition === 'prefix') {
      return `${signPrefix}${currencyFormat.symbol}${space}${formattedAmount}`;
    } else {
      return `${signPrefix}${formattedAmount}${space}${currencyFormat.symbol}`;
    }
  }, [formattedAmount, currencyFormat, isNegative, showPositiveSign]);
  
  // Determine CSS classes for styling
  const cssClasses = useMemo(() => {
    const baseClasses = 'font-medium tabular-nums';
    const negativeClass = isNegative && highlightNegative ? 'text-red-600 dark:text-red-400' : '';
    const customClass = className || '';
    
    return [baseClasses, negativeClass, customClass]
      .filter(Boolean)
      .join(' ');
  }, [isNegative, highlightNegative, className]);
  
  return (
    <span className={cssClasses} title={`${amount} ${currency}`}>
      {displayAmount}
    </span>
  );
};

export default PaymentAmountDisplay;