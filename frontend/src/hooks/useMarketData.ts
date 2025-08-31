import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMarketDataStore, useQuotes, useOrderBooks, useTrades } from '@/store/marketDataStore';
import { Quote, OrderBookSnapshot, Trade } from '@/types/marketData';

// Query keys for React Query
export const marketDataKeys = {
  all: ['marketData'] as const,
  quotes: () => [...marketDataKeys.all, 'quotes'] as const,
  orderBooks: () => [...marketDataKeys.all, 'orderBooks'] as const,
  trades: () => [...marketDataKeys.all, 'trades'] as const,
  symbol: (symbol: string) => [...marketDataKeys.all, 'symbol', symbol] as const,
  quote: (symbol: string) => [...marketDataKeys.all, 'quote', symbol] as const,
  orderBook: (symbol: string) => [...marketDataKeys.all, 'orderBook', symbol] as const,
  symbolTrades: (symbol: string) => [...marketDataKeys.all, 'trades', symbol] as const,
};

// Hook for getting all quotes
export const useQuotesQuery = () => {
  const quotes = useQuotes();
  
  return useQuery({
    queryKey: marketDataKeys.quotes(),
    queryFn: () => Object.values(quotes),
    initialData: Object.values(quotes),
    staleTime: 0, // Always consider data stale for real-time updates
    refetchInterval: false, // Disable automatic refetching
  });
};

// Hook for getting all order books
export const useOrderBooksQuery = () => {
  const orderBooks = useOrderBooks();
  
  return useQuery({
    queryKey: marketDataKeys.orderBooks(),
    queryFn: () => Object.values(orderBooks),
    initialData: Object.values(orderBooks),
    staleTime: 0,
    refetchInterval: false,
  });
};

// Hook for getting all trades
export const useTradesQuery = () => {
  const trades = useTrades();
  
  return useQuery({
    queryKey: marketDataKeys.trades(),
    queryFn: () => trades,
    initialData: trades,
    staleTime: 0,
    refetchInterval: false,
  });
};

// Hook for getting data for a specific symbol
export const useSymbolQuery = (symbol: string) => {
  const quotes = useQuotes();
  const orderBooks = useOrderBooks();
  const trades = useTrades();
  
  const quote = quotes[symbol];
  const orderBook = orderBooks[symbol];
  const symbolTrades = trades[symbol] || [];
  
  return useQuery({
    queryKey: marketDataKeys.symbol(symbol),
    queryFn: () => ({
      symbol,
      quote,
      orderBook,
      trades: symbolTrades,
    }),
    initialData: {
      symbol,
      quote,
      orderBook,
      trades: symbolTrades,
    },
    staleTime: 0,
    refetchInterval: false,
    enabled: !!symbol,
  });
};

// Hook for getting quote for a specific symbol
export const useQuoteQuery = (symbol: string) => {
  const quotes = useQuotes();
  const quote = quotes[symbol];
  
  return useQuery({
    queryKey: marketDataKeys.quote(symbol),
    queryFn: () => quote,
    initialData: quote,
    staleTime: 0,
    refetchInterval: false,
    enabled: !!symbol,
  });
};

// Hook for getting order book for a specific symbol
export const useOrderBookQuery = (symbol: string) => {
  const orderBooks = useOrderBooks();
  const orderBook = orderBooks[symbol];
  
  return useQuery({
    queryKey: marketDataKeys.orderBook(symbol),
    queryFn: () => orderBook,
    initialData: orderBook,
    staleTime: 0,
    refetchInterval: false,
    enabled: !!symbol,
  });
};

// Hook for getting trades for a specific symbol
export const useSymbolTradesQuery = (symbol: string) => {
  const trades = useTrades();
  const symbolTrades = trades[symbol] || [];
  
  return useQuery({
    queryKey: marketDataKeys.symbolTrades(symbol),
    queryFn: () => symbolTrades,
    initialData: symbolTrades,
    staleTime: 0,
    refetchInterval: false,
    enabled: !!symbol,
  });
};

// Hook for invalidating market data queries
export const useInvalidateMarketData = () => {
  const queryClient = useQueryClient();
  
  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: marketDataKeys.all });
  };
  
  const invalidateSymbol = (symbol: string) => {
    queryClient.invalidateQueries({ queryKey: marketDataKeys.symbol(symbol) });
  };
  
  const invalidateQuotes = () => {
    queryClient.invalidateQueries({ queryKey: marketDataKeys.quotes() });
  };
  
  const invalidateOrderBooks = () => {
    queryClient.invalidateQueries({ queryKey: marketDataKeys.orderBooks() });
  };
  
  const invalidateTrades = () => {
    queryClient.invalidateQueries({ queryKey: marketDataKeys.trades() });
  };
  
  return {
    invalidateAll,
    invalidateSymbol,
    invalidateQuotes,
    invalidateOrderBooks,
    invalidateTrades,
  };
};
