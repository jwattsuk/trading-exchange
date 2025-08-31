import { useMarketDataStore, useQuotes, useOrderBooks, useTrades } from '@/store/marketDataStore';

// Simple hooks that directly use Zustand selectors without React Query
// This prevents the infinite loop issue caused by mixing React Query with Zustand

// Hook for getting all quotes
export const useQuotesQuery = () => {
  const quotes = useQuotes();
  
  return {
    data: Object.values(quotes),
    isLoading: false,
    error: null,
  };
};

// Hook for getting all order books
export const useOrderBooksQuery = () => {
  const orderBooks = useOrderBooks();
  
  return {
    data: Object.values(orderBooks),
    isLoading: false,
    error: null,
  };
};

// Hook for getting all trades
export const useTradesQuery = () => {
  const trades = useTrades();
  
  return {
    data: Object.values(trades),
    isLoading: false,
    error: null,
  };
};

// Hook for getting data for a specific symbol
export const useSymbolQuery = (symbol: string) => {
  const quotes = useQuotes();
  const orderBooks = useOrderBooks();
  const trades = useTrades();
  
  const quote = quotes[symbol];
  const orderBook = orderBooks[symbol];
  const symbolTrades = trades[symbol] || [];
  
  return {
    data: {
      symbol,
      quote,
      orderBook,
      trades: symbolTrades,
    },
    isLoading: false,
    error: null,
  };
};

// Hook for getting quote for a specific symbol
export const useQuoteQuery = (symbol: string) => {
  const quotes = useQuotes();
  const quote = quotes[symbol];
  
  return {
    data: quote,
    isLoading: false,
    error: null,
  };
};

// Hook for getting order book for a specific symbol
export const useOrderBookQuery = (symbol: string) => {
  const orderBooks = useOrderBooks();
  const orderBook = orderBooks[symbol];
  
  return {
    data: orderBook,
    isLoading: false,
    error: null,
  };
};

// Hook for getting trades for a specific symbol
export const useSymbolTradesQuery = (symbol: string) => {
  const trades = useTrades();
  const symbolTrades = trades[symbol] || [];
  
  return {
    data: symbolTrades,
    isLoading: false,
    error: null,
  };
};

// Hook for invalidating market data queries (no-op since we're not using React Query)
export const useInvalidateMarketData = () => {
  const invalidateAll = () => {
    // No-op - Zustand handles updates automatically
  };
  
  const invalidateSymbol = (symbol: string) => {
    // No-op - Zustand handles updates automatically
  };
  
  const invalidateQuotes = () => {
    // No-op - Zustand handles updates automatically
  };
  
  const invalidateOrderBooks = () => {
    // No-op - Zustand handles updates automatically
  };
  
  const invalidateTrades = () => {
    // No-op - Zustand handles updates automatically
  };
  
  return {
    invalidateAll,
    invalidateSymbol,
    invalidateQuotes,
    invalidateOrderBooks,
    invalidateTrades,
  };
};
